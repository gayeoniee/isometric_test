package com.daengs.app.miniroom

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.daengs.app.miniroom.art.DoorSpec
import com.daengs.app.miniroom.art.ItemCatalog
import com.daengs.app.miniroom.art.drawFence
import com.daengs.app.miniroom.art.drawRoomShell
import com.daengs.app.miniroom.sprite.rememberFrameClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.sin

/**
 * 방 전체를 그리는 **하나의** Canvas.
 *
 * 왜 하나여야 하나: 화가 알고리즘(뒤에서 앞으로 덧그리기)으로 앞뒤를 표현하려면
 * 벽·바닥·가구·강아지가 같은 draw 패스에 있어야 한다. 그래서 터치 판정과 드래그를
 * Compose 레이아웃에 맡기지 못하고 직접 짠다.
 *
 * @param frameTimeMs null 이 아니면 애니메이션 프레임을 그 시각으로 고정한다.
 *   @Preview 에서는 무한 애니메이션이 돌지 않아 프레임 0 에 얼어붙으므로 필요하다.
 * @param onItemTap null 이 아니면 **끌지 않고 톡 누른** 아이템을 넘겨준다 (방향 돌리기).
 *   길게 누르기가 아니라 탭으로 구분하는 이유: 드래그가 슬롭 없이 즉시 시작되므로
 *   길게 누르기를 기다리면 끌기가 그만큼 늦게 붙어 손맛이 나빠진다.
 * @param onDoorOpened 문이 **활짝 열린 순간** 불린다. 산책 게임이 생기면 여기서 화면을 넘기면 된다.
 * @param doorOpenOverride null 이 아니면 문 열림 정도를 그 값으로 고정한다 (@Preview 용).
 */
@Composable
fun MiniRoomCanvas(
    state: MiniRoomState,
    catalog: ItemCatalog,
    theme: RoomTheme = RoomTheme.DEFAULT,
    modifier: Modifier = Modifier,
    frameTimeMs: Long? = null,
    onItemTap: ((PlacedItem) -> Unit)? = null,
    onDoorOpened: (() -> Unit)? = null,
    doorOpenOverride: Float? = null,
) {
    val clock = rememberFrameClock()

    // 0 = 닫힘, 1 = 활짝
    val doorOpen = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val doorCallback by rememberUpdatedState(onDoorOpened)
    // 여는 중에 또 눌러도 애니메이션이 겹치지 않게 Job 하나로 관리한다
    val doorJob = remember { arrayOfNulls<Job>(1) }

    fun openDoor() {
        if (doorJob[0]?.isActive == true) return
        doorJob[0] = scope.launch {
            doorOpen.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            // 산책 게임이 생기면 이 시점에 화면 전환 -> 닫히는 구간은 안 보이게 된다
            doorCallback?.invoke()
            delay(420)
            doorOpen.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
        }
    }

    // 콜백을 pointerInput 키로 직접 쓰면 안 된다. 람다는 재구성마다 새 객체라
    // 제스처 코루틴이 매번 재시작되고, 끌던 중이면 그대로 죽는다.
    // 키는 Boolean 으로 고정하고 콜백은 최신 것을 읽는다.
    val tapHandler by rememberUpdatedState(onItemTap)
    val tapEnabled = onItemTap != null

    Spacer(
        modifier
            // 키는 반드시 Unit. state.items 같은 걸 키로 주면 아이템을 놓는 순간
            // 제스처 코루틴이 재시작되면서 드래그가 도중에 죽는다.
            .pointerInput(tapEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val g = RoomGeometry.of(size.width.toFloat(), size.height.toFloat())

                    // 아이템이 먼저다. 문 앞에 놓인 물건을 누르면 물건이 반응해야 한다.
                    if (!state.beginDrag(down.position, g, catalog)) {
                        // 아이템이 아니면 문인지 본다.
                        if (DoorSpec.contains(g, down.position)) {
                            // down 을 소비하지 않는다 — 문 위에서 쓸어내리면
                            // (나중에 스크롤이 생겼을 때) 스크롤이 되게 두려는 것.
                            var slid = false
                            while (true) {
                                val e = awaitPointerEvent()
                                val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                                if ((ch.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    slid = true
                                }
                                if (!ch.pressed) break
                            }
                            if (!slid) openDoor()
                        }
                        // 아이템도 문도 아니면 제스처를 포기한다.
                        return@awaitEachGesture
                    }

                    // 아이템을 정확히 눌렀으므로 슬롭 없이 바로 잡는다.
                    // 여기서 소비해야 부모 스크롤이 가로채지 않는다.
                    down.consume()
                    val grabbed = state.drag?.instanceId
                    var moved = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                moved = true
                            }
                            state.updateDrag(
                                change.position,
                                RoomGeometry.of(size.width.toFloat(), size.height.toFloat()),
                                catalog,
                            )
                            change.consume()
                        }
                        val onTap = tapHandler
                        if (!moved && onTap != null && grabbed != null) {
                            // 제자리 탭 → 이동이 아니라 인벤토리로 보내기
                            state.cancelDrag()
                            state.items.firstOrNull { it.instanceId == grabbed }?.let(onTap)
                        } else {
                            state.endDrag(catalog)
                        }
                    } catch (e: CancellationException) {
                        state.cancelDrag()
                        throw e
                    }
                }
            }
            .drawBehind {
                // 상태는 전부 draw 람다 **안에서** 읽는다. 그래야 recomposition 없이
                // draw 단계만 무효화돼 드래그 중에도 프레임이 안 떨어진다.
                val g = RoomGeometry.of(size.width, size.height)
                val t = frameTimeMs ?: clock.value
                val d = state.drag
                val open = doorOpenOverride ?: doorOpen.value
                // 누를 수 있다는 은은한 표시. 열리기 시작하면 꺼진다.
                val pulse = ((sin(t / 900f) + 1f) / 2f) * (1f - open)

                drawRoomShell(g, theme, open, pulse)

                if (d != null) {
                    val dragged = state.items.firstOrNull { it.instanceId == d.instanceId }
                    val box = dragged?.let { catalog[it.itemId]?.box }
                    if (box != null && !d.willRemove) {
                        drawCellGhost(g, d.targetCol, d.targetRow, box.footprint, d.valid)
                        drawLiftShadow(g, d.targetCol, d.targetRow, box.footprint)
                    }
                }

                val order = state.drawOrder(catalog)

                fun DrawScope.paint(item: PlacedItem) {
                    val art = catalog[item.itemId] ?: return
                    if (d != null && d.instanceId == item.instanceId) {
                        drawItem(art, item, g, t, dragOffset = d.visualDelta, lift = 7f * g.scale)
                    } else {
                        drawItem(art, item, g, t)
                    }
                }

                // 1) 보통 아이템 — col+row 순서대로
                for (item in order) {
                    if (catalog[item.itemId]?.box?.alwaysOnTop == true) continue
                    paint(item)
                }

                // 2) 울타리는 바닥 앞쪽 모서리에 서므로 아이템보다 뒤에 그리면 안 된다.
                drawFence(g, theme)

                // 3) 강아지 — 울타리보다도 앞. 주인공이라 무엇에도 가리지 않는다.
                for (item in order) {
                    if (catalog[item.itemId]?.box?.alwaysOnTop != true) continue
                    paint(item)
                }

                // 4) 방 밖으로 끌어낸 상태면 손끝에 치우기 표시
                if (d != null && d.willRemove) drawRemoveHint(d.pointer, g)
            }
    )
}
