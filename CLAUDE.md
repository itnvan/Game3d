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
- **Vật liệu thật**: `Mat.tex` (enum `Tex`) trỏ tới `assets/tex/<name>_a.jpg` + `_n.jpg` (CC0, nguồn trong
  `assets/tex/CREDITS.md`). Ảnh đã chuẩn hoá sáng trung tính → màu do vertex color quyết. Texture **không** nhúng
  GLB: `TextureBank` gắn texture dùng chung sau khi tạo model. Thêm texture: chạy lại công cụ chuẩn hoá (giống các
  ảnh có sẵn), thêm `Tex(asset, normalScale)`, đặt `texScale` (mét/lần lặp) ở preset `Mat`.
- **Bầu trời/ánh sáng**: mái vòm `WorldModel.skyDome()` dùng `tex/sky_day|night.jpg`; SH trong `SceneSetup` và
  `tex/sky_day_reflect.bin` sinh từ cùng HDRI với cùng góc xoay `SKY_AZIMUTH` — đổi HDRI phải sinh lại cả ba.
- **Hoạt động/tiến độ**: `core/game/activities` (câu cá, bọ, nấu ăn), `core/game/progress` (nhiệm vụ, điểm danh,
  sưu tập, lễ hội). Mọi tiến độ nhiệm vụ đi qua `GameWorld.progress(kind, n)`.
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
9. Primitive trong GLB phải cùng thứ tự với `Glb.primitiveTextures(mesh)` (bỏ part rỗng): `TextureBank` gắn theo
   chỉ số primitive. Đổi cách `Glb.encode` lọc part thì sửa cả hai.
10. Debug lễ hội: `adb shell am start -n com.cozyhome.game/com.cozyhome.app.MainActivity --es festival HALLOWEEN`
    (chuỗi rỗng để về lịch thật). Debug giờ: `--ef hour 21.5`. Vườn đủ 7 loại cây: `--es garden x`. Kho nông sản:
    `--es pantry x`.
11. **Không sinh ảnh/mesh trên luồng chính**: `TextureGen.image`, `build()` mesh đều chạy ở `Dispatchers.Default`;
    luồng chính chỉ upload/`createModelInstance` (cắt lát qua `MeshLoad`). Đo treo luồng chính bằng cách chụp
    `Looper.getMainLooper().thread.stackTrace` từ 1 thread phụ khi tick main trễ > 150 ms.
12. Camera chơi chúc 30° nên **không nhìn thấy trời**: thứ gì trên cao (pháo hoa) phải đặt trước camera và chỉ
    thấy ở `SKY_VIEW_RIG` (nút Pháo hoa đêm lễ hội).
13. Ánh sáng đêm theo lux thật: trăng 2 600 lux. Đèn điểm dưới ~100 000 lm gần như vô hình ngoài trời — chỉnh
    `LampSpot.lumens`, đừng chỉnh ánh trăng.
14. Khi điện thoại khoá: soi bằng emulator `PDFReaderDev` (1080×2400, `-gpu host -no-window -no-snapshot-save
    -port 5580`). Máy ảo chậm hơn máy thật nhiều lần — dùng để soi hình/luồng, không đo FPS. Sau khi khởi động
    nguội có màn khoá (screencap ra đen): `adb -s emulator-5580 shell input keyevent 82`. Tắt máy ảo trước khi build
    release (R8 cần RAM).
