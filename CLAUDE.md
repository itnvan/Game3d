# Game3d — Cozy Home 3D

Game 3D Android: mỗi người chơi có 1 căn nhà để trang trí, 1 vườn rau, và sân chơi thể thao (bóng rổ, sút
penalty, chạy bộ). Miễn phí, bản test (chưa có ads/IAP/backend). Khởi tạo 2026-10-17.

applicationId `com.cozyhome.game`, namespace `com.cozyhome.app`, minSdk 24, compile/target 37, landscape.
Stack: Kotlin 2.4.20 + Compose (BOM 2026.09.00) + SceneView 4.57 (Filament 1.72) + Koin + kotlinx.serialization.
Kiến trúc theo plan "Splat Explorer" (zip owner gửi): logic game là module **pure Kotlin** `core/game`, app chỉ
nối logic với SceneView. Chi tiết thiết kế: `docs/design.md`. Lịch sử quyết định: `docs/CHANGELOG.md`.

## Hợp đồng kiến trúc

- `core/game` không phụ thuộc Android/SceneView. Mọi luật chơi (va chạm, camera, vườn, thể thao, decor, kinh tế)
  ở đây và có unit test JVM (`./gradlew :core:game:test`).
- **Model 3D sinh bằng code**: `core/game/mesh/MeshBuilder` (hình khối + màu theo đỉnh) → `Glb.encode` → GLB
  bytes → `modelLoader.createModelInstance`. Không có file .glb trong assets. Thêm/sửa model ở `core/game/models/*`.
- **Soi model trên PC**: `./gradlew :core:game:test` xuất ảnh `core/game/build/sheets/*.png` (raster phần mềm
  `raster/Thumbnail`). Mở ảnh ra xem trước khi cài máy. Thumbnail cửa hàng cũng dùng raster này (cache ở
  `cacheDir/thumbs_v<VERSION>` — đổi model thì tăng `ThumbCache.VERSION`).
- **Game loop**: `PlayScreen` chạy `withFrameNanos` → `GameViewModel.tick` → `GameWorld.step` → `FrameApplier`
  ghi transform thẳng vào node (không qua Compose state). HUD chỉ đọc `HudState` (đổi tần suất thấp; meter/stamina
  đọc trong draw).
- **Ref tới node phải được dọn khi node bị huỷ** (`SceneRefs`, `RigRefs.clear`, `Holder` trong `WorldScene`). Ghi
  transform vào node đã destroy = crash native.
- Dữ liệu lưu: `SaveData` (JSON `filesDir/save.json`, ghi nguyên tử, debounce 700 ms + flush ON_STOP). Đổi field:
  luôn có default, không đổi tên field đã phát hành.
- **Âm thanh sinh bằng code** (`core/game/audio` → `app/audio/Mixer`), không có file âm thanh. Tiếng mới: thêm
  `Sfx` + công thức trong `SfxSynth`, nối sự kiện ở `SoundCues` (có test trong `AudioTest`), nghe lại
  `core/game/build/sounds/*.wav`. Nếu buộc phải dùng file thì chỉ CC0 và ghi nguồn trong `docs/design.md`.

## Bẫy đã biết

1. SceneView build bằng JVM 21 → app phải `jvmTarget 21` (không thì lỗi inline bytecode).
2. `FrameRatePolicy.OnDemand`: setter của node/light tự xin frame; ghi thẳng Filament (skybox, IBL, fog) phải
   `invalidator.requestRender()`.
3. Emoji trong UI phải có từ Unicode ≤ 9 (Android 7). Đừng dùng 🧺 🧢 🧶 🟥 🪙…
4. Máy test OPPO CPH2481 (Mali-G57) khoá màn hình bằng vân tay; `pm clear` bị chặn → xoá save bằng
   `adb shell run-as com.cozyhome.game rm -f files/save.json`.
5. Comment trong code: không narration; chỉ 1 dòng cho bẫy không đọc được từ code.
6. Bản **debug** render âm thanh chậm ~4–6× bản release (ART chạy app debuggable bằng interpreter chậm): lần mở đầu
   sau mỗi lần cài, SFX có sau ~10 s, nhạc sau ~18 s; các lần sau đọc `PcmCache`. Đó không phải lỗi.
7. `Pcm` chỉ xử lý tới `written`: chạy bộ lọc IIR qua đoạn im lặng làm đuôi rơi vào số denormal, chậm gấp nhiều lần.
8. `Lot.PATHS`/`GARDEN_PATH` (chọn tiếng bước chân) là bản chép toạ độ lối đi trong `WorldModel.paths()` /
   `gardenGround()`: dời lối đi thì sửa cả hai.
