package com.cozyhome.game.decor

import com.cozyhome.game.math.DEG
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.state.PlacedItem
import com.cozyhome.game.state.Zone
import com.cozyhome.game.world.Lot
import com.cozyhome.game.world.Rect
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

object Placement {
    private const val CELL = Catalog.CELL

    fun zoneRect(zone: Zone): Rect = if (zone == Zone.HOUSE) Lot.ROOM else Lot.YARD

    fun zoneOf(rule: ZoneRule) = if (rule == ZoneRule.HOUSE) Zone.HOUSE else Zone.YARD

    fun yaw(rot: Int): Float = ((rot % 4 + 4) % 4) * 90f

    fun cellsW(def: ItemDef, rot: Int) = if (rot % 2 == 0) def.w else def.d
    fun cellsD(def: ItemDef, rot: Int) = if (rot % 2 == 0) def.d else def.w

    fun footprint(def: ItemDef, x: Float, z: Float, rot: Int): Rect {
        val w = cellsW(def, rot) * CELL
        val d = cellsD(def, rot) * CELL
        return Rect(x - w / 2f, z - d / 2f, x + w / 2f, z + d / 2f)
    }

    fun footprint(item: PlacedItem): Rect? = Catalog[item.itemId]?.let { footprint(it, item.x, item.z, item.rot) }

    fun snap(def: ItemDef, rot: Int, zone: Zone, want: V2): V2 {
        val r = zoneRect(zone)
        val w = cellsW(def, rot)
        val d = cellsD(def, rot)
        val cellsX = ((r.maxX - r.minX) / CELL).roundToInt()
        val cellsZ = ((r.maxZ - r.minZ) / CELL).roundToInt()
        val kx = floor((want.x - w * CELL / 2f - r.minX) / CELL + 0.5f).toInt().coerceIn(0, (cellsX - w).coerceAtLeast(0))
        val kz = floor((want.z - d * CELL / 2f - r.minZ) / CELL + 0.5f).toInt().coerceIn(0, (cellsZ - d).coerceAtLeast(0))
        return V2(r.minX + (kx + w / 2f) * CELL, r.minZ + (kz + d / 2f) * CELL)
    }

    fun isValid(def: ItemDef, pos: V2, rot: Int, zone: Zone, placed: List<PlacedItem>, ignoreUid: Long = -1L): Boolean {
        if (zoneOf(def.zone) != zone) return false
        val fp = footprint(def, pos.x, pos.z, rot)
        val zr = zoneRect(zone)
        if (fp.minX < zr.minX - 1e-3f || fp.maxX > zr.maxX + 1e-3f || fp.minZ < zr.minZ - 1e-3f || fp.maxZ > zr.maxZ + 1e-3f) return false
        if (zone == Zone.HOUSE && def.layer == Layer.OBJECT && fp.overlaps(Lot.DOOR_CLEAR)) return false
        for (other in placed) {
            if (other.uid == ignoreUid || other.zone != zone) continue
            val od = Catalog[other.itemId] ?: continue
            if (od.layer != def.layer) continue
            if (fp.overlaps(footprint(od, other.x, other.z, other.rot))) return false
        }
        return true
    }

    fun findSpot(def: ItemDef, rot: Int, zone: Zone, placed: List<PlacedItem>, near: V2): V2? {
        val zr = zoneRect(zone)
        val start = snap(def, rot, zone, near)
        if (isValid(def, start, rot, zone, placed)) return start
        for (ring in 1..40) {
            for (dx in -ring..ring) for (dz in -ring..ring) {
                if (kotlin.math.abs(dx) != ring && kotlin.math.abs(dz) != ring) continue
                val p = snap(def, rot, zone, V2(start.x + dx * CELL, start.z + dz * CELL))
                if (p.x < zr.minX || p.x > zr.maxX || p.z < zr.minZ || p.z > zr.maxZ) continue
                if (isValid(def, p, rot, zone, placed)) return p
            }
        }
        return null
    }

    fun toWorld(item: PlacedItem, local: V3): V3 {
        val a = yaw(item.rot) * DEG
        val c = cos(a)
        val s = sin(a)
        return V3(item.x + local.x * c + local.z * s, local.y, item.z - local.x * s + local.z * c)
    }

    fun frontOf(item: PlacedItem, distance: Float): V2 {
        val a = yaw(item.rot) * DEG
        return V2(item.x + sin(a) * distance, item.z + cos(a) * distance)
    }

    fun solids(placed: List<PlacedItem>): List<Rect> = placed.mapNotNull { item ->
        val def = Catalog[item.itemId] ?: return@mapNotNull null
        if (def.layer == Layer.FLOOR) return@mapNotNull null
        footprint(def, item.x, item.z, item.rot).inset(0.08f)
    }
}
