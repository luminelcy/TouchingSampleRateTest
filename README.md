# TouchingSampleRateTest

Android 触摸采样率测试工具。实时显示设备的触摸采样率，并提供可视化拖尾效果来直观感受触控跟手性。

## 功能

- **实时采样率显示** — 同时展示两种采样率：
  - `raw`：仅统计 MotionEvent 回调的时间戳（受 InputDispatcher 投递超时和 UI 线程调度间隔影响）
  - `w/ history`：包含 `getHistoricalEventTime` 中的历史采样点，更接近硬件真实采样率
- **Unbuffered 开关** — 调用 `requestUnbufferedDispatch`（Android 13+）取消 InputDispatcher 的投递超时等待，使触摸事件逐点投递
- **Trail 开关** — 显示触摸拖尾轨迹，绿色线条为轨迹，红色圆点为每个采样点，可直观判断触控是否平滑
- **自适应布局** — 根据屏幕宽度自动缩放字号，兼容手机和平板

## 原理

Android 触摸事件在 InputReader、InputDispatcher 层会被短暂积攒后批量投递，中间的采样点作为历史点嵌入 `MotionEvent` 中。通过 `getHistoricalEventTime` 还原这些历史点，即可测量真实的硬件触摸采样率。

`requestUnbufferedDispatch`（Android 13+）可以告知 InputDispatcher 跳过投递超时等待，将触摸事件逐点立即投递给应用。这能降低输入数据到达应用的延迟，但不会降低视觉延迟，因为画面渲染仍受 VSync 周期和渲染管线的约束。

## 环境要求

- Android Studio Hedgehog 或更高版本
- minSdk 24
- Unbuffered 功能需要 Android 13（API 33）及以上设备
