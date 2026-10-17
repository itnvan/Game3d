# Cozy Home 3D — thiết kế

| Trường | Giá trị |
|---|---|
| Ngày | 2026-10-17 |
| Phiên bản | 0.1.0 (bản test miễn phí) |
| Nền tảng | Android 7+ (minSdk 24), màn ngang |
| Repo | https://github.com/itnvan/Game3d |

## Phạm vi đã chốt với owner (2026-10-17)

- Game 3D trên **app Android** (owner xác nhận), miễn phí, đang test — chưa ads/IAP/đăng nhập.
- Mỗi người chơi có **1 căn nhà** (trang trí nội thất), **sân/vườn trồng rau**, **các hoạt động thể thao**.
- Màu sắc tươi mới (pastel bão hoà, low-poly bo tròn).
- Kiến trúc lấy từ plan "Splat Explorer" (zip owner gửi): logic game pure Kotlin tách module, SceneView render,
  game loop ghi thẳng transform vào node.

## Chưa làm (cố ý)

- Nhiều người chơi / thăm nhà nhau (cần backend). Hiện mỗi máy 1 căn nhà, lưu local.
- Âm thanh (chưa có asset âm thanh có bản quyền rõ ràng).
- Ads, IAP, Firebase/Crashlytics (chưa có `google-services.json`).
- Keystore release: bản release đang ký bằng debug key.

## Bản đồ khu đất (mét, X đông, Z nam, Y lên)

```
          z=-20  ┌──────────────── hàng rào ────────────────┐
                 │ [Sân bóng rổ]   [  NHÀ 12×10  ]  [Vườn]  │
                 │  x -20..-10      x -6..6          x 9..19.5│
                 │  z -17..-7       z -16..-6        z -16..-6│
                 │            ╭──── đường chạy 58 m ────╮    │
                 │ [Sân bóng] │   bãi cỏ "Ngoài sân"    │ ao │
                 │  khung thành│   (decor YARD)          │    │
                 │  x=-21      ╰────────────────────────╯    │
          z=20   └──────────────── cổng + hộp thư ───────────┘
```

Toạ độ chuẩn nằm ở `core/game/world/Lot.kt`. Va chạm: `CollisionWorld` (đẩy ra khỏi hình chữ nhật/tròn,
chia bước theo bán kính → không xuyên tường).

## Vòng chơi

| Hoạt động | Cách chơi | Thưởng |
|---|---|---|
| Vườn rau | Gieo hạt (tốn xu) → tưới mỗi giai đoạn (3 lần) → thu hoạch. Thời gian thực, chạy cả khi tắt app nhưng mỗi lần tưới chỉ lớn 1 giai đoạn. Vòi nước "Tưới cả vườn". | Xu + XP |
| Bóng rổ | 10 lượt, thanh lực dao động, vùng xanh thu hẹp theo chuỗi. Vật lý bóng thật (vành, bảng, lưới). | Điểm = xu |
| Sút penalty | 5 quả, vuốt lên → điểm cuối vuốt (raycast vào mặt khung thành) là đích, tốc độ vuốt là lực. Thủ môn đoán hướng có xác suất. | 10 xu/bàn |
| Chạy bộ | 3 vòng qua 8 cổng/vòng, giữ Tăng tốc (thể lực), ra khỏi đường chạy bị chậm. Huy chương theo thời gian. | 6–45 xu |
| Trang trí | Cửa hàng 46 món (32 trong nhà, 14 ngoài sân), lưới 0,5 m, xoay 90°, đổi màu, cất vào túi, sơn tường 7 màu, sàn 5 kiểu. | — |
| Đồ dùng được | Ngồi (sofa/ghế/ghế băng), ngủ (giường → ban đêm thì tua tới 7h sáng), bật TV, bật đèn, bạt nhún (combo), xích đu (đẩy theo nhịp). | XP |
| Quà hằng ngày | Hộp thư cạnh cổng, 1 lần/ngày. | 60 + 20×cấp xu |

Cấp độ: XP cần cho cấp L = 30·(L−1)·L. Một số món đồ/hạt giống khoá theo cấp.

Ngày/đêm: 1 giờ game = 50 s thật (1 ngày = 20 phút), trời đổi màu sáng/hoàng hôn/đêm, có sương mù cùng màu trời.

## Reference

- Plan gốc: `game_3d.zip` → `splat-explorer-plan/261016 - DEV - Splat Explorer Plan.md` (SceneView, core/game pure
  Kotlin, WalkArea, FollowCamera, game loop `withFrameNanos`).
- SceneView API: https://sceneview.github.io/docs/llms.txt (đã đọc bản 4.57.0 ngày 2026-10-17).
- Gradle/version copy theo `D:\AndroidStudioProjects\Palmume` (AGP 9.3.1, Kotlin 2.4.20, Gradle 9.8.0) để dùng
  cache sẵn.
- Máy test: OPPO CPH2481, Mali-G57 MC2, OpenGL ES 3.2.

## Checklist

- [x] Spike: GLB sinh bằng code + màu đỉnh + bóng đổ trên máy thật (GO).
- [x] Lõi: va chạm, camera theo, nhân vật + animation thủ tục, ngày/đêm, lưu game.
- [x] Nhà: sàn, tường 2 lớp màu, cửa sổ kính, mái, tường tự hạ khi camera bị che.
- [x] Trang trí trong nhà + ngoài sân, cửa hàng có thumbnail 3D.
- [x] Vườn 12 luống, 7 loại cây, 4 giai đoạn.
- [x] Bóng rổ, penalty, chạy bộ, bạt nhún, xích đu.
- [x] HUD, tạo nhân vật, cài đặt, song ngữ Việt/Anh.
- [x] Test toàn bộ trên máy thật (OPPO CPH2481): đi lại, trang trí, vườn, bóng rổ, penalty, chạy bộ, ngồi, ngủ, bạt nhún, xích đu, quà, đổi nhân vật.
- [ ] Âm thanh, nhiều người chơi, thăm nhà bạn bè.

## Log tiến độ

- 2026-10-17: dựng repo, spike GO trên OPPO, viết toàn bộ core + model + app, unit test core xanh.
- 2026-10-17: test máy thật, sửa bố cục cho màn thấp, tối ưu render (~15 → ~48 FPS), sửa 8 lỗi hiển thị.
