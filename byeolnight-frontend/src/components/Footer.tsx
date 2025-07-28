import { useTranslation } from 'react-i18next';

export default function Footer() {
  const { t } = useTranslation();
  
  return (
    <footer className="bg-[#1f2336]/80 text-gray-400 text-center text-sm py-4 mt-12">
      <p>© 2025 {t('home.title')} – {t('home.footer_text')}</p>
    </footer>
  );
}
