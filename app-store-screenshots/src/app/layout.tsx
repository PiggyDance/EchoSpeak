import type { Metadata } from "next";
import { Noto_Sans_SC } from "next/font/google";
import "./globals.css";

const notoSansSC = Noto_Sans_SC({ 
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "900"]
});

export const metadata: Metadata = {
  title: "EchoSpeak App Store Screenshots",
  description: "Generate App Store screenshots for EchoSpeak",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body className={notoSansSC.className}>
        {children}
      </body>
    </html>
  );
}
