import { useTranslation } from 'react-i18next';

export default function LanguageSwitcher() {
  const { i18n } = useTranslation();

  return (
    <select 
      value={i18n.language} 
      onChange={(e) => i18n.changeLanguage(e.target.value)}
      className="px-3 py-2 rounded-full bg-slate-800/80 hover:bg-slate-700/80 text-white border border-purple-500/30 hover:border-purple-400/50 transition-all duration-200 text-sm cursor-pointer focus:outline-none focus:ring-2 focus:ring-purple-400/50"
    >
      <option value="ko">🇰🇷 한국어</option>
      <option value="ja">🇯🇵 日本語</option>
      <option value="en">🇺🇸 English</option>
    </select>
  );
}