# Changelog

## 2026-10-18 — chạy thử trên máy ảo: sửa lỗi nạp, đêm, pháo hoa

Điện thoại test bị khoá nên chạy trên emulator `PDFReaderDev` (1080×2400, GPU host) để soi từng màn.

- **Texture sinh bằng code (lá, nước, chấm, trời) tạo trên luồng chính** trong `TextureBank.upload` → treo 2–3 s
  lúc mở màn (bắt bằng watchdog chụp stack luồng chính). Chuyển `TextureGen.image` sang `Dispatchers.Default`, luồng
  chính chỉ upload.
- **Nhân vật hiện từng mảnh lúc nạp** (mắt lơ lửng, thân không đầu): `CharacterRig` chỉ dựng body khi đủ mọi bộ phận
  và trả về `Boolean` đã đủ; màn tạo nhân vật có lớp phủ trời + vòng xoay, mờ dần khi sân khấu + rig + texture xong
  (chốt 1 lần, đổi đồ sau đó không nháy). `rememberMeshInstance` gán `null` khi mesh mới rỗng thay vì giữ mesh cũ.
- **Đèn đường không hắt sáng**: 24 000 lm chỉ ~1/8 ánh trăng (2 600 lux) nên vô hình. Mỗi cột đèn có lumen riêng:
  đèn đường 600 000 lm (quầng ~4–5 m bằng ánh trăng), đèn hiên 70 000 lm (sát tường, mạnh hơn là cháy trắng). Đèn đồ
  đạc 60 000 (trong nhà) / 120 000 (ngoài vườn) — luôn bật nên giữ mức ban ngày gần như không thấy.
- **Cửa sổ sáng ấm ban đêm**: mỗi tường có thêm tấm unlit 1 mặt hướng ra ngoài sau lớp kính, alpha theo độ tối
  (`windowGlow`). Chỉ gắn vào tường nguyên nên vào trong nhà (tường cắt) không bị che cửa.
- **Pháo hoa không bao giờ thấy được**: bắn ở sau lưng camera, cao 13–20 m trong khi camera chúc 30° chỉ thấy tới
  ~8° dưới chân trời. Pháo hoa giờ nổ phía trước camera (20–30 m, cao 8–14 m) + nút "Pháo hoa" chỉ hiện vào đêm lễ hội:
  camera hạ thấp ngước lên (`SKY_VIEW_RIG`), chạm cần điều khiển là về như cũ. Hạt pháo đổi từ cầu trắng (emissive 6
  bị tone map thành trắng) sang tia màu emissive 1,8 + mờ dần bằng `emissiveFactor`.
- Bếp: món chưa đủ cấp hiện ổ khoá "Lv N" thay vì nút xám không rõ lý do. Album: thẻ cá luôn chừa dòng "Best" để hàng
  đều. Hàng nút hoạt động không còn đè thẻ tên. Chuỗi `\x27` (hiện "Todayx27s") thành `\'`. Biển phát sáng trên cửa
  (ban ngày như khối vàng lạc chỗ) thành biển gỗ viền đồng.

## 2026-10-18 — đồ hoạ "thật" + câu cá, bắt bọ, nấu ăn, lễ hội, nhiệm vụ

- **Texture chụp thật CC0 (Poly Haven, ambientCG) — owner chọn** khi hỏi "giống ngoài đời 100%": model sinh bằng code
  không thể thành ảnh chụp trên Mali-G57 mà vẫn mượt, nên giữ hình khối sinh bằng code + phủ vật liệu thật (cỏ, gỗ sàn,
  ngói, vữa, vải, đất, đá, đường chạy cao su…). Ảnh được **chuẩn hoá về sáng trung tính** rồi nhân với màu đỉnh: cùng
  1 texture dùng được cho mọi biến thể màu của đồ đạc. Bỏ chuẩn hoá theo từng kênh vì texture bão hoà (sàn gỗ đỏ, thảm
  xanh) có kênh gần 0 → nhiễu hạt; chuẩn hoá theo độ sáng + giới hạn độ lệch màu ±33%.
- **Texture dùng chung, không nhúng vào GLB**: gltfio tạo texture riêng cho mỗi model, 40 món đồ × 2–3 ảnh 1K là hàng
  trăm MB GPU. GLB chỉ nhúng ảnh giữ chỗ 2×2 (để gltfio bật `baseColorIndex`/`normalIndex` và tính tangent), rồi
  `TextureBank` gắn texture dùng chung vào `baseColorMap`/`normalMap` theo thứ tự primitive (`Glb.primitiveTextures`).
  Ảnh nạp 1 cái mỗi frame để màn loading không khựng.
- **Bầu trời là ảnh HDRI thật trên mái vòm unlit** (đổi texture ngày/đêm, nhuộm màu theo giờ) thay vì skybox màu phẳng;
  ánh sáng môi trường là **SH 9 hệ số tính sẵn từ chính HDRI** + cubemap phản chiếu 64px tự lọc (không prefilter lúc
  chạy — rẻ, chắc chắn chạy trên Mali). Mái vòm và SH xoay cùng 1 góc để mặt trời trên ảnh khớp hướng nắng. Fog có
  `cutOffDistance` để không phủ mất mái vòm. Tone mapping PBR Neutral (giữ màu tươi, không bệt như ACES).
- **Nhân vật làm lại kiểu Play Together**: đầu to, mắt nhiều lớp (tròng màu, đồng tử, 2 điểm sáng, mi, chớp mắt bằng
  scale node mắt), lông mày, má hồng mềm (quad alpha), 8 kiểu tóc ghép từ "mũ tóc" (mép mái lượn vào trong đầu) + lọn
  tóc dẹt bám theo bề mặt đầu, 8 mũ, 6 kiểu áo, 3 kiểu quần/váy, 3 kiểu mắt, 7 màu mắt. Đội mũ thì tóc tự nén lại
  để không xuyên mũ. `Appearance` thêm field có default nên save cũ vẫn mở được.
- **Cảnh**: địa hình heightfield mượt (hồ là chỗ lõm thật, hết đè cỏ lên nước), cây thân cong có cành + tán phồng đổ
  sáng/tối theo chiều cao, thông nhiều tầng, cỏ mọc thành bụi quanh hàng rào/gốc cây, hoa mọc theo cụm, đèn đường có
  đèn chiếu sáng thật ban đêm (point light, chỉ bật khi tối). Đêm tối hẳn (ambient 0,14) thay vì sáng như ngày.
- **Nội thất 46 món + nhà làm lại**: gỗ vân, vải lanh/nhung/da, gốm, đồng, chân côn, gối phồng, sách thật trên kệ,
  rèm cửa, mái ngói đất nung, ống khói gạch. Đồ đổ bóng mềm (blob) dưới chân.
- **Cây trồng thật**: lá là tấm cong 2 mặt (`leafBlade`, có khe cho monstera), xà lách cuốn, cà chua có giàn, bắp
  có lá rủ, bí ngô có múi. Thu hoạch giờ còn cho 2 nông sản vào kho để nấu ăn.
- **Hoạt động mới** (logic thuần Kotlin có test): câu cá ở hồ (quăng → chờ, phao nhấp nhử → phao chìm thì giật →
  giữ để kéo, thả khi dây đỏ; 12 loài theo giờ/độ hiếm, có rác và rương báu), bắt 10 loài côn trùng (bướm, đom đóm
  đêm, bọ cánh cứng chiều tối…), nấu 8 món ở bếp từ rau nhà trồng, 8 biểu cảm (nhảy, vỗ tay…).
- **Giữ chân người chơi offline** (owner: chỉ chơi local): 3 nhiệm vụ mỗi ngày sinh theo ngày, điểm danh 7 ngày (ngày 7
  tặng cúp vàng), bộ sưu tập cá/bọ/món/nông sản có mốc thưởng (đồ trang trí độc quyền), **lễ hội theo lịch thật**
  (Tết, Valentine, hoa xuân, hè, Trung thu, Halloween, Giáng sinh — Tết/Trung thu tra bảng âm lịch 2025–2030): trang trí
  cả khu, xu lễ hội, cửa hàng đồ lễ hội, pháo hoa ban đêm. Lịch tự tính (không dùng java.time vì minSdk 24).
- **UI**: bộ icon vector tự vẽ thay emoji hệ thống, nút bóng (gradient + viền sáng + vệt bóng), màn tạo nhân vật có
  tab + lưới ảnh xem trước thật từng kiểu, màn loading có tiến độ thật (texture + model) và mẹo chơi.

## 2026-10-18 — âm thanh + nhạc nền (sinh bằng code)

- **Tự tổng hợp thay vì dùng file âm thanh**: không có asset nào có bản quyền rõ ràng, và cùng hướng với model 3D
  sinh bằng code — APK không phình, chỉnh âm = sửa số, và chạy được trên JVM nên `AudioTest` xuất `.wav` +
  `sfx_sheet.png` (dạng sóng + spectrogram) để soi trước khi lên máy. 40 SFX + 2 bài nhạc (ngày 96 BPM, đêm 66 BPM
  nhịp 3/4 hộp nhạc), viết nốt bằng chuỗi `"E5:1 G5:.5 …"` để đọc/sửa được như bản nhạc.
- **Nhạc lặp không vấp**: render thêm 4 s đuôi rồi cộng gấp vào đầu bài, reverb chạy 2 lượt (lượt 1 chỉ để làm ấm
  bộ lọc) → mẫu cuối nối mẫu đầu liền mạch; test `songsLoopSeamlessly` khoá điều này.
- **Mixer tự viết trên 1 `AudioTrack`** thay vì `SoundPool`: SoundPool chỉ nạp từ file/fd, và cần thêm crossfade
  ngày/đêm 3 s, hạ nhạc khi phát jingle (lên cấp, kết quả, bàn thắng), đổi pitch theo cue (cổng chạy bộ leo
  thang ngũ cung, bạt nhún cao dần theo combo). Track mono 48 kHz chế độ low-latency (~50 ms trên OPPO), tự
  `pause()` khi im 1 s, tạo lại track nếu `write` trả lỗi. Nhạc render ở 24 kHz để nửa RAM/CPU.
- **Chậm khi render trên máy**: bản đầu 10,7 s cho SFX. Một phần do đuôi bộ lọc IIR rơi vào số denormal trong ~2,5 s
  im lặng của mỗi buffer (JVM 307 → 93 ms sau khi chỉ xử lý tới `written`). Phần lớn còn lại là do bản **debug**:
  ART chạy app debuggable bằng interpreter chậm — bản release trên OPPO chỉ 2,8 s SFX + 1,4 s nhạc. Vì vậy có
  `PcmCache` (PCM 16-bit trong `cacheDir`, khoá theo mtime của APK nên cài bản mới là tự render lại, không có hằng
  VERSION để quên tăng), luồng render ở mức ưu tiên nền, mỗi âm phát được ngay khi render xong. Mở lại: 0,26 s.
- **Bước chân theo nhịp animation** (`Animator.steps` tăng ở điểm chân duỗi xa nhất) nên khớp hình (~8 bước/s khi
  chạy); mặt đất: gỗ trong nhà, cứng (hiên đá, lối đi, sân bóng rổ, đường chạy), còn lại là cỏ. Toạ độ lối đi
  chép vào `Lot.PATHS`/`GARDEN_PATH` mà không sửa `WorldModel`, để nhánh làm lại đồ hoạ merge không đụng file model.
- **Âm của toast đi qua `GameViewModel.toast()`**: dùng luôn chống lặp 1,2 s của toast, và toast chỉ phát từ VM
  (mua/đặt/cất đồ, thiếu xu, khoá cấp) có tiếng theo cùng bảng `SoundCues.of(Msg)`. Trong 1 frame mỗi âm chỉ phát
  1 lần; lên cấp nuốt jingle kết quả, hết vòng nuốt tiếng cổng.
- Bạt nhún tự nảy mỗi ~0,65 s nên tiếng tiếp đất thường hạ còn 0,35 và trầm hơn, chỉ cú nảy đúng nhịp mới "boing" to.
  Nút Nhảy/Đẩy/Ném không phát tiếng bấm (đã có tiếng riêng, bấm liên tục).
- Loa điện thoại gần như không ra dưới ~250 Hz → thêm lớp 250–340 Hz cho tiếng sút, bóng nảy, bóng rơi cỏ, đặt đồ.
- Cài đặt: nút Âm thanh/Nhạc nằm cùng hàng nút Đóng vì hộp thoại đã cao 267/320 dp trên OPPO. Lưu ở prefs `audio`,
  không trong `SaveData`, nên "Chơi lại từ đầu" không bật lại tiếng người chơi đã tắt.
- Chưa làm: audio focus (nhạc game vẫn chạy chung khi app khác đang phát nhạc), âm thanh nổi theo vị trí.

## 2026-10-17 — test trên máy thật (OPPO CPH2481, Mali-G57, density 540, font 1.15)

- **Màn ngang chỉ cao ~320 dp** trên máy này (density 540 + cỡ chữ 1.15) → toàn bộ HUD thu gọn, cửa hàng trang
  trí chuyển thành **cột lưới bên trái** thay vì dải dưới đáy: dải dưới che mất nửa căn phòng, còn cột trái để
  phòng chiếm trọn bên phải (camera dịch `decorShift` sang trái khi panel mở, về 0 khi đang sửa đồ).
- **Chạm chọn đồ dò theo chiều cao món đồ** (`decorTap(origin, dir)`): tia chạm cắt mặt sàn phía sau sofa/tủ nên
  chạm vào nửa trên món đồ trước đây không chọn được.
- **Hiệu năng**: mặc định của SceneView (SSAO + bloom + bóng 2 cascade, full 2400×1080) chỉ ~15 FPS. Đo từng yếu
  tố bằng extra debug `gfx`: tắt bóng → 59 FPS, scale 0.4 → 60 FPS, ẩn cảnh → +3 FPS. Chốt: tắt SSAO/MSAA, HDR
  LOW, bóng 1 cascade 1024, cảnh ngoài hàng rào tách mesh riêng không đổ bóng, độ phân giải cố định theo tier.
  `QualityGovernor` đo khoảng cách frame thật (SceneView `onFrame`) và hạ tier khi < 45 FPS, lưu vào prefs.
  Không dùng dynamic resolution tự động vì Mali này bật workaround `dont_use_timer_query` (không có GPU timing).
- **Penalty**: người sút đứng chéo cạnh bóng + camera đặt sau quả bóng — camera sau lưng người sút làm đầu nhân
  vật che mất bóng.
- **Bóng rổ**: hạ điểm cầm bóng xuống 1,42 m (trước lơ lửng trên đầu) thì góc rơi chỉ ~32° và chạm vành trước →
  tăng góc ném 64°. Test `perfectBasketballShotScores` bắt được lỗi này trước khi lên máy.
- Đèn hiên, biển cửa và cánh cửa chuyển vào model tường nam: khi tường hạ xuống lúc vào nhà chúng không còn lơ lửng.
- Cổng checkpoint chạy bộ bỏ `+90°`: cột cổng đang nằm dọc theo đường chạy, cắm xuyên người chơi.
- Ghế xích đu xoay ngược dấu với vị trí người ngồi (`Rotation(x)` quay phần treo về −z).

## 2026-10-17 — v0.1.0 bản test đầu tiên

- **Model 3D sinh bằng code thay vì file .glb**: không có asset 3D có bản quyền rõ ràng, và mỗi món đồ cần
  nhiều biến thể màu. `MeshBuilder` + `Glb.encode` cho phép đổi màu/kiểu tóc/đồ đạc ngay trong game, mỗi model
  chỉ 1–3 draw call (màu theo đỉnh `COLOR_0`, nhóm theo vật liệu), nhẹ hơn nhiều so với ghép hàng trăm
  `CubeNode`. Spike trên Mali-G57 xác nhận gltfio hiển thị đúng màu đỉnh + bóng đổ.
- **Thumbnail cửa hàng bằng raster phần mềm** (`raster/Thumbnail`) thay vì chụp offscreen bằng Filament: chạy
  trên JVM nên dùng luôn cho test xuất ảnh soi model, và không cần SceneView thứ hai trong UI.
- **Chấm penalty lùi ra 7 m, bóng 8,5–17,5 m/s**: ở 4,8 m với 23 m/s bóng tới khung trong 0,2 s, nhỏ hơn thời
  gian phản xạ thủ môn → 40/40 cú sút góc đều vào (đo bằng test). Sau chỉnh: thủ môn cản ~15% cú sút góc.
- **Bóng rổ ném góc 58°, nhắm sau tâm vành 3,5 cm, vành 0,26 m**: góc 55° khiến bóng rơi xuống quá thoải và đập
  vành trước dù lực chuẩn. Test `perfectBasketballShotScores` khoá lại hành vi này.
- **Tường nhà tự hạ khi che camera** (2 bản model: cao 3 m và cắt 0,55 m, đổi `isVisible`) thay vì làm tường
  trong suốt: Filament blend nhiều lớp kính/tường dễ lỗi thứ tự vẽ, còn tường cắt giữ được nhìn nội thất rõ.
- **JVM 21**: SceneView 4.57 build bằng JVM 21, app để 17 thì lỗi "Cannot inline bytecode built with JVM
  target 21".
- Dọn tham chiếu node khi node rời composition (`Holder`, `RigRefs.clear`): game loop ghi transform mỗi frame,
  ghi vào node đã destroy là crash native.
