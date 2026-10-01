import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'MiniWallet Hub - Cổng Quản Trị Hệ Thống',
  description: 'Hệ thống quản trị ví điện tử, giám sát thanh toán VietQR và đối soát sổ cái kép ACID',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
