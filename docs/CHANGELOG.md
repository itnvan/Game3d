# Changelog

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
