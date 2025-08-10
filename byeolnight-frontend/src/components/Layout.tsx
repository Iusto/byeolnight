import Navbar from './Navbar';
import Footer from './Footer';
import { Outlet } from 'react-router-dom';

export default function Layout() {
  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white font-sans">
      <Navbar />
      <main className="flex-1 px-4 py-8 max-w-6xl mx-auto w-full">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
