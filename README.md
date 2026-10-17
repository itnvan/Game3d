# Cozy Home 3D 🏡

Game 3D trên Android: mỗi người chơi có **một căn nhà để trang trí**, **một vườn rau** để trồng – tưới – thu
hoạch, và **sân chơi thể thao** (bóng rổ, sút penalty, chạy bộ, bạt nhún, xích đu). Đồ hoạ low-poly bo tròn,
màu pastel tươi. Bản test miễn phí, chưa có quảng cáo/IAP, dữ liệu lưu trên máy.

## Chơi gì

| | |
|---|---|
| 🛋️ **Trang trí** | Cửa hàng 46 món (32 trong nhà, 14 ngoài sân), đặt theo lưới 0,5 m, kéo thả, xoay, đổi màu, cất vào túi. Sơn tường 7 màu, 5 kiểu sàn. |
| 🌱 **Vườn rau** | 12 luống, 7 loại cây. Gieo → tưới mỗi giai đoạn → chín → thu hoạch lấy xu. Cây lớn theo thời gian thực. Vòi nước "tưới cả vườn". |
| 🏀 **Bóng rổ** | 10 lượt, canh vạch lực vào vùng xanh, vật lý bóng thật (vành, bảng, lưới). |
| ⚽ **Penalty** | 5 quả, vuốt lên về phía khung thành (điểm cuối là đích, tốc độ vuốt là lực), thủ môn biết đổ người. |
| 🏃 **Chạy bộ** | 3 vòng qua 8 cổng/vòng, giữ Tăng tốc, huy chương vàng/bạc/đồng. |
| 🤸 **Đồ chơi** | Ngồi sofa/ghế, ngủ (ban đêm thì tua tới sáng), bật TV/đèn, bạt nhún (combo), xích đu. |
| 🎁 **Hằng ngày** | Quà ở hộp thư, ngày/đêm (1 ngày game = 20 phút), lên cấp mở khoá đồ mới. |

## Kiến trúc

```
core/game   Kotlin thuần (JVM), có unit test
  mesh/     MeshBuilder (hình khối + màu theo đỉnh) → Glb.encode (GLB bytes)
  models/   toàn bộ model 3D sinh bằng code: nhà, nội thất, rau, nhân vật, cảnh
  raster/   raster phần mềm → thumbnail cửa hàng + ảnh soi model khi test
  world/    khu đất, va chạm, camera theo, đường chạy, ngày/đêm
  sports/   bóng rổ, penalty, chạy bộ
  game/     GameWorld: điều phối input → luật chơi → FrameState
app         Compose + SceneView 4.57 (Filament): PlayScreen, HUD, tạo nhân vật
```

Không có file 3D nào trong assets — mọi model được dựng bằng code rồi nạp vào Filament qua GLB.

## Build

```bash
./gradlew :core:game:test
```

```bash
./gradlew :app:assembleDebug
```

`core:game:test` xuất ảnh soi model ở `core/game/build/sheets/*.png`.

Yêu cầu: JDK 21+ (Android Studio JBR), Android SDK 37. minSdk 24 (Android 7), màn ngang.

### Lệnh debug (chỉ bản debug)

```bash
adb shell am start -n com.cozyhome.game/com.cozyhome.app.MainActivity --ef tp_x 10.6 --ef tp_z -8 --ef tp_yaw 180
```

Extra hỗ trợ: `tp_x/tp_z/tp_yaw` (dịch chuyển), `coins`, `xp`, `hour`, `grow` (tua cây), `gfx` + `scale/shadows/post/scenery` (đo hiệu năng).

## Hiệu năng

`QualityGovernor` tự hạ độ phân giải render (0.8 → 0.46) khi FPS < 45 và nhớ mức đã chọn. Trên OPPO CPH2481
(Mali-G57 MC2) game chạy ~45–50 FPS ở mức 0.56, có bóng đổ.

Chi tiết thiết kế: `docs/design.md`. Lịch sử quyết định: `docs/CHANGELOG.md`.
