"use client";

import { toPng } from "html-to-image";
import { useEffect, useRef, useState } from "react";

// ============================================================================
// CONSTANTS & DESIGN TOKENS
// ============================================================================

const W = 1320; // Design width (6.9" iPhone)
const H = 2868; // Design height

const SIZES = [
  { label: '6.9"', w: 1320, h: 2868 },
  { label: '6.5"', w: 1284, h: 2778 },
  { label: '6.3"', w: 1206, h: 2622 },
  { label: '6.1"', w: 1125, h: 2436 },
] as const;

// Mockup measurements (pre-calculated from mockup.png)
const MK_W = 1022;
const MK_H = 2082;
const SC_L = (52 / MK_W) * 100;
const SC_T = (46 / MK_H) * 100;
const SC_W = (918 / MK_W) * 100;
const SC_H = (1990 / MK_H) * 100;
const SC_RX = (126 / 918) * 100;
const SC_RY = (126 / 1990) * 100;

// Brand colors - 蓝紫色深色主题
const COLORS = {
  primary: "#7C3AED", // 紫色
  secondary: "#3B82F6", // 蓝色
  accent: "#A78BFA", // 浅紫色
  dark: "#0F172A", // 深色背景
  darker: "#020617", // 更深背景
  text: "#F1F5F9", // 浅色文字
  textDim: "#94A3B8", // 灰色文字
};

// ============================================================================
// PHONE COMPONENT
// ============================================================================

function Phone({
  src,
  alt,
  style,
  className = "",
}: {
  src: string;
  alt: string;
  style?: React.CSSProperties;
  className?: string;
}) {
  return (
    <div
      className={`relative ${className}`}
      style={{ aspectRatio: `${MK_W}/${MK_H}`, ...style }}
    >
      <img
        src="/mockup.png"
        alt=""
        className="block w-full h-full"
        draggable={false}
      />
      <div
        className="absolute z-10 overflow-hidden"
        style={{
          left: `${SC_L}%`,
          top: `${SC_T}%`,
          width: `${SC_W}%`,
          height: `${SC_H}%`,
          borderRadius: `${SC_RX}% / ${SC_RY}%`,
        }}
      >
        <img
          src={src}
          alt={alt}
          className="block w-full h-full object-cover object-top"
          draggable={false}
        />
      </div>
    </div>
  );
}

// ============================================================================
// CAPTION COMPONENT
// ============================================================================

function Caption({
  label,
  headline,
  color = COLORS.text,
}: {
  label?: string;
  headline: string;
  color?: string;
}) {
  return (
    <div className="text-center">
      {label && (
        <div
          className="font-semibold tracking-wider uppercase mb-3"
          style={{
            fontSize: `${W * 0.028}px`,
            color: COLORS.accent,
            letterSpacing: "0.1em",
          }}
        >
          {label}
        </div>
      )}
      <h1
        className="font-bold leading-tight"
        style={{
          fontSize: `${W * 0.095}px`,
          lineHeight: 1.0,
          color,
        }}
      >
        {headline}
      </h1>
    </div>
  );
}

// ============================================================================
// DECORATIVE COMPONENTS - 温暖有机风格
// ============================================================================

function Blob({
  style,
  color,
  opacity = 0.15,
}: {
  style?: React.CSSProperties;
  color: string;
  opacity?: number;
}) {
  return (
    <div
      className="absolute rounded-full blur-3xl"
      style={{
        background: color,
        opacity,
        ...style,
      }}
    />
  );
}

function Glow({
  style,
  color,
}: {
  style?: React.CSSProperties;
  color: string;
}) {
  return (
    <div
      className="absolute rounded-full"
      style={{
        background: `radial-gradient(circle, ${color}40 0%, transparent 70%)`,
        filter: "blur(60px)",
        ...style,
      }}
    />
  );
}

// ============================================================================
// SCREENSHOT 1: HERO - 实时回声,练习说话
// ============================================================================

function Screenshot1() {
  return (
    <div
      className="relative overflow-hidden"
      style={{
        width: `${W}px`,
        height: `${H}px`,
        background: `linear-gradient(135deg, ${COLORS.darker} 0%, ${COLORS.dark} 100%)`,
      }}
    >
      {/* 装饰性光晕 */}
      <Glow
        color={COLORS.primary}
        style={{
          width: "800px",
          height: "800px",
          top: "-200px",
          right: "-150px",
        }}
      />
      <Glow
        color={COLORS.secondary}
        style={{
          width: "600px",
          height: "600px",
          bottom: "200px",
          left: "-100px",
        }}
      />

      {/* Blob 装饰 */}
      <Blob
        color={COLORS.primary}
        opacity={0.2}
        style={{
          width: "500px",
          height: "500px",
          top: "150px",
          right: "-100px",
        }}
      />

      {/* App Icon */}
      <div
        className="absolute left-1/2 -translate-x-1/2"
        style={{ top: "180px" }}
      >
        <img
          src="/app-icon.png"
          alt="EchoSpeak"
          className="rounded-3xl shadow-2xl"
          style={{
            width: "200px",
            height: "200px",
            filter: "drop-shadow(0 30px 60px rgba(124, 58, 237, 0.4))",
          }}
        />
      </div>

      {/* Caption */}
      <div
        className="absolute left-1/2 -translate-x-1/2"
        style={{ top: "450px", width: "90%" }}
      >
        <Caption
          label="语音练习神器"
          headline={
            <>
              实时回声
              <br />
              练习说话更轻松
            </>
          }
        />
      </div>

      {/* Phone */}
      <Phone
        src="/screenshots/screen-recording.jpg"
        alt="主界面"
        className="absolute left-1/2 -translate-x-1/2"
        style={{
          bottom: 0,
          width: "84%",
          transform: "translateX(-50%) translateY(13%)",
          filter: "drop-shadow(0 40px 80px rgba(0, 0, 0, 0.6))",
        }}
      />
    </div>
  );
}

// ============================================================================
// SCREENSHOT 2: 说完即听 - 1秒自动播放
// ============================================================================

function Screenshot2() {
  return (
    <div
      className="relative overflow-hidden"
      style={{
        width: `${W}px`,
        height: `${H}px`,
        background: `linear-gradient(160deg, ${COLORS.primary}15 0%, ${COLORS.dark} 40%, ${COLORS.darker} 100%)`,
      }}
    >
      {/* 装饰光晕 */}
      <Glow
        color={COLORS.accent}
        style={{
          width: "700px",
          height: "700px",
          top: "100px",
          left: "-150px",
        }}
      />

      {/* Caption */}
      <div
        className="absolute left-1/2 -translate-x-1/2"
        style={{ top: "300px", width: "85%" }}
      >
        <Caption
          label="即时反馈"
          headline={
            <>
              说完即听
              <br />
              1秒后自动播放
            </>
          }
        />
      </div>

      {/* 两个手机叠加效果 */}
      <Phone
        src="/screenshots/screen-listening.jpg"
        alt="监听界面"
        className="absolute"
        style={{
          left: "-8%",
          bottom: "0",
          width: "68%",
          transform: "rotate(-4deg) translateY(10%)",
          opacity: 0.6,
          filter: "drop-shadow(0 30px 60px rgba(0, 0, 0, 0.5))",
        }}
      />
      <Phone
        src="/screenshots/screen-playing.jpg"
        alt="播放界面"
        className="absolute"
        style={{
          right: "-5%",
          bottom: "0",
          width: "84%",
          transform: "translateY(12%)",
          filter: "drop-shadow(0 50px 100px rgba(0, 0, 0, 0.7))",
        }}
      />
    </div>
  );
}

// ============================================================================
// SCREENSHOT 3: 科幻视觉 - 动态音频可视化
// ============================================================================

function Screenshot3() {
  return (
    <div
      className="relative overflow-hidden"
      style={{
        width: `${W}px`,
        height: `${H}px`,
        background: `linear-gradient(225deg, ${COLORS.dark} 0%, ${COLORS.secondary}20 50%, ${COLORS.darker} 100%)`,
      }}
    >
      {/* 多个光晕营造科幻氛围 */}
      <Glow
        color={COLORS.secondary}
        style={{
          width: "800px",
          height: "800px",
          top: "-100px",
          right: "-200px",
        }}
      />
      <Glow
        color={COLORS.primary}
        style={{
          width: "600px",
          height: "600px",
          bottom: "300px",
          left: "50%",
        }}
      />

      {/* Blob装饰 */}
      <Blob
        color={COLORS.accent}
        opacity={0.18}
        style={{
          width: "450px",
          height: "450px",
          top: "500px",
          left: "-100px",
        }}
      />

      {/* Caption */}
      <div
        className="absolute left-1/2 -translate-x-1/2"
        style={{ top: "250px", width: "85%" }}
      >
        <Caption
          label="沉浸体验"
          headline={
            <>
              科幻视觉效果
              <br />
              动态音频可视化
            </>
          }
        />
      </div>

      {/* Phone */}
      <Phone
        src="/screenshots/screen-recording.jpg"
        alt="可视化界面"
        className="absolute left-1/2 -translate-x-1/2"
        style={{
          bottom: 0,
          width: "86%",
          transform: "translateX(-50%) translateY(14%)",
          filter: "drop-shadow(0 60px 120px rgba(59, 130, 246, 0.4))",
        }}
      />
    </div>
  );
}

// ============================================================================
// SCREENSHOT 4: 完美音质 - 清晰还原
// ============================================================================

function Screenshot4() {
  return (
    <div
      className="relative overflow-hidden"
      style={{
        width: `${W}px`,
        height: `${H}px`,
        background: `linear-gradient(135deg, ${COLORS.darker} 0%, ${COLORS.primary}10 50%, ${COLORS.dark} 100%)`,
      }}
    >
      {/* 装饰光晕 */}
      <Glow
        color={COLORS.accent}
        style={{
          width: "900px",
          height: "900px",
          top: "50%",
          left: "50%",
          transform: "translate(-50%, -50%)",
        }}
      />

      {/* Caption */}
      <div
        className="absolute left-1/2 -translate-x-1/2"
        style={{ top: "280px", width: "88%" }}
      >
        <Caption
          label="高品质音频"
          headline={
            <>
              清晰的音频质量
              <br />
              完美还原你的声音
            </>
          }
        />
      </div>

      {/* App Icon 小图标 */}
      <div
        className="absolute left-1/2 -translate-x-1/2"
        style={{ bottom: "280px" }}
      >
        <img
          src="/app-icon.png"
          alt="EchoSpeak"
          className="rounded-2xl opacity-90"
          style={{
            width: "120px",
            height: "120px",
            filter: "drop-shadow(0 20px 40px rgba(124, 58, 237, 0.3))",
          }}
        />
      </div>

      {/* 功能标签 */}
      <div
        className="absolute left-1/2 -translate-x-1/2 flex flex-wrap gap-3 justify-center"
        style={{ bottom: "120px", width: "85%" }}
      >
        {[
          "实时回声",
          "自动播放",
          "音频可视化",
          "降噪处理",
          "高清录音",
          "多语言支持",
        ].map((feature, i) => (
          <div
            key={i}
            className="px-6 py-3 rounded-full font-medium"
            style={{
              background: `linear-gradient(135deg, ${COLORS.primary}30, ${COLORS.secondary}30)`,
              backdropFilter: "blur(10px)",
              border: `1px solid ${COLORS.accent}40`,
              color: COLORS.text,
              fontSize: "28px",
            }}
          >
            {feature}
          </div>
        ))}
      </div>
    </div>
  );
}

// ============================================================================
// SCREENSHOTS REGISTRY
// ============================================================================

const SCREENSHOTS = [
  { id: 1, name: "hero", component: Screenshot1 },
  { id: 2, name: "instant-playback", component: Screenshot2 },
  { id: 3, name: "visualization", component: Screenshot3 },
  { id: 4, name: "audio-quality", component: Screenshot4 },
];

// ============================================================================
// SCREENSHOT PREVIEW WITH EXPORT
// ============================================================================

function ScreenshotPreview({
  screenshot,
  index,
}: {
  screenshot: (typeof SCREENSHOTS)[0];
  index: number;
}) {
  const previewRef = useRef<HTMLDivElement>(null);
  const offscreenRef = useRef<HTMLDivElement>(null);
  const [scale, setScale] = useState(1);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    const updateScale = () => {
      if (previewRef.current) {
        const containerWidth = previewRef.current.offsetWidth;
        setScale(containerWidth / W);
      }
    };

    const observer = new ResizeObserver(updateScale);
    if (previewRef.current) {
      observer.observe(previewRef.current);
    }

    return () => observer.disconnect();
  }, []);

  const exportScreenshot = async (size: (typeof SIZES)[0]) => {
    if (!offscreenRef.current) return;

    setExporting(true);
    const el = offscreenRef.current;

    try {
      // Move on-screen temporarily
      el.style.left = "0px";
      el.style.opacity = "1";
      el.style.zIndex = "-1";

      const opts = { width: W, height: H, pixelRatio: 1, cacheBust: true };

      // Double-call trick to warm up fonts/images
      await toPng(el, opts);
      await new Promise((r) => setTimeout(r, 100));
      const dataUrl = await toPng(el, opts);

      // Resize to target size
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement("canvas");
        canvas.width = size.w;
        canvas.height = size.h;
        const ctx = canvas.getContext("2d")!;
        ctx.drawImage(img, 0, 0, size.w, size.h);

        canvas.toBlob((blob) => {
          if (blob) {
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `${String(index + 1).padStart(2, "0")}-${
              screenshot.name
            }-${size.w}x${size.h}.png`;
            a.click();
            URL.revokeObjectURL(url);
          }
        });
      };
      img.src = dataUrl;

      // Move back off-screen
      el.style.left = "-9999px";
      el.style.opacity = "";
      el.style.zIndex = "";

      await new Promise((r) => setTimeout(r, 300));
    } catch (err) {
      console.error("Export failed:", err);
      alert("导出失败,请重试");
      el.style.left = "-9999px";
    } finally {
      setExporting(false);
    }
  };

  const Component = screenshot.component;

  return (
    <div className="bg-gray-800 rounded-lg overflow-hidden shadow-xl">
      {/* Preview */}
      <div
        ref={previewRef}
        className="relative bg-gray-900 overflow-hidden"
        style={{ aspectRatio: `${W}/${H}` }}
      >
        <div
          style={{
            transform: `scale(${scale})`,
            transformOrigin: "top left",
            width: `${W}px`,
            height: `${H}px`,
          }}
        >
          <Component />
        </div>
      </div>

      {/* Export Buttons */}
      <div className="p-4 space-y-2">
        <div className="text-white font-semibold mb-3">
          {index + 1}. {screenshot.name}
        </div>
        <div className="grid grid-cols-2 gap-2">
          {SIZES.map((size) => (
            <button
              key={size.label}
              onClick={() => exportScreenshot(size)}
              disabled={exporting}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 text-white rounded-lg text-sm font-medium transition-colors"
            >
              {exporting ? "导出中..." : `${size.label} (${size.w}×${size.h})`}
            </button>
          ))}
        </div>
      </div>

      {/* Offscreen render */}
      <div
        ref={offscreenRef}
        className="absolute pointer-events-none"
        style={{
          left: "-9999px",
          top: 0,
          fontFamily: "inherit",
        }}
      >
        <Component />
      </div>
    </div>
  );
}

// ============================================================================
// MAIN PAGE
// ============================================================================

export default function ScreenshotsPage() {
  const [exportingAll, setExportingAll] = useState(false);

  const exportAll = async () => {
    setExportingAll(true);
    for (let i = 0; i < SCREENSHOTS.length; i++) {
      const btns = document.querySelectorAll(
        `[data-screenshot-index="${i}"] button`
      );
      if (btns[0]) {
        (btns[0] as HTMLButtonElement).click();
        await new Promise((r) => setTimeout(r, 1000));
      }
    }
    setExportingAll(false);
  };

  return (
    <div className="min-h-screen bg-gray-950 p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8 text-center">
          <h1 className="text-4xl font-bold text-white mb-4">
            EchoSpeak App Store 截图
          </h1>
          <p className="text-gray-400 mb-6">
            点击按钮导出不同尺寸的截图,适用于 Apple App Store
          </p>
          <button
            onClick={exportAll}
            disabled={exportingAll}
            className="px-8 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 text-white rounded-lg font-semibold text-lg transition-colors"
          >
            {exportingAll ? "批量导出中..." : "导出全部 (6.9\")"}
          </button>
        </div>

        {/* Screenshots Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {SCREENSHOTS.map((screenshot, index) => (
            <div key={screenshot.id} data-screenshot-index={index}>
              <ScreenshotPreview screenshot={screenshot} index={index} />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
