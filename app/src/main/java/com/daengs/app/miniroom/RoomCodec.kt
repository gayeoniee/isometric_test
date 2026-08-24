package com.daengs.app.miniroom

/**
 * 방 배치를 문자열 한 줄로 바꾸는 코덱.
 *
 * 안드로이드 API 를 안 쓰는 **순수 함수**라 단위 테스트로 고정할 수 있다.
 * 저장소(SharedPreferences 든 DataStore 든 서버든)는 이 문자열만 들고 있으면 된다.
 *
 * 형식: `v1;instanceId,itemId,col,row,facing;...`
 *
 * 맨 앞 버전 표시가 있는 이유: 나중에 필드가 늘면 옛 데이터를 읽다가 깨진다.
 * 버전이 다르면 조용히 null 을 돌려주고 기본 배치로 시작하는 편이,
 * 잘못 해석해서 아이템이 엉뚱한 칸에 박히는 것보다 낫다.
 */
object RoomCodec {

    /**
     * v2: 강아지가 격자에서 빠졌다 (DogHerd 로 이동).
     * v1 저장본에는 "dog" 가 칸을 차지한 채 들어있어서, 그대로 읽으면
     * 격자 강아지 + 돌아다니는 강아지가 둘 다 그려진다. 버전을 올려 버린다.
     */
    private const val VERSION = "v3"
    private const val RECORD = ';'
    private const val FIELD = ','

    fun encode(items: List<PlacedItem>): String = buildString {
        append(VERSION)
        items.forEach { i ->
            append(RECORD)
            append(i.instanceId).append(FIELD)
            append(i.itemId).append(FIELD)
            append(i.col).append(FIELD)
            append(i.row).append(FIELD)
            append(i.facing)
        }
    }

    /** 못 읽으면 null. 호출한 쪽이 기본 배치로 넘어가게 한다. */
    fun decode(raw: String?): List<PlacedItem>? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(RECORD)
        if (parts.firstOrNull() != VERSION) return null

        val out = ArrayList<PlacedItem>(parts.size - 1)
        for (rec in parts.drop(1)) {
            if (rec.isBlank()) continue
            val f = rec.split(FIELD)
            if (f.size != 5) return null
            val id = f[0].toLongOrNull() ?: return null
            val col = f[2].toIntOrNull() ?: return null
            val row = f[3].toIntOrNull() ?: return null
            val facing = f[4].toIntOrNull() ?: return null
            if (f[1].isBlank()) return null
            // 격자 밖 값이 들어오면 통째로 버린다. 한 줄만 이상해도
            // 방 전체가 이상해 보이므로 부분 복구는 오히려 헷갈린다.
            if (col !in 0 until RoomSpec.GRID || row !in 0 until RoomSpec.GRID) return null
            out += PlacedItem(id, f[1], col, row, facing.coerceIn(0, PlacedItem.FACINGS - 1))
        }
        return out
    }
}
