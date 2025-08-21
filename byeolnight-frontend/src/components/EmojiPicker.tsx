import { useState } from 'react';

interface Props {
  onEmojiSelect: (emoji: string) => void;
  className?: string;
}

const EMOJI_CATEGORIES: Record<string, string[]> = {
  '우주': ['🌌', '🌟', '⭐', '🌠', '🚀', '🛸', '🌙', '🌕', '🌖', '🌗', '🌘', '🌑', '🌒', '🌓', '🌔', '🌍', '🌎', '🌏', '🪐', '🛰', '💫', '✨', '🎆', '🎇', '🌜', '🌛', '🌝', '🌞', '☀️', '⛅', '☁️', '⚡', '🌈', '☄️'],
  '감정': ['😊', '😂', '🥰', '😍', '🤔', '😮', '😢', '😭', '😡', '🤯', '😎', '🥳', '😴', '🤗', '👍', '😄', '😁', '😆', '😅', '🤣', '😉', '😌', '😋', '😛', '😜', '😝', '🤑', '🤓', '😒', '😐', '😑', '😶', '🙄', '😯', '😦', '😧', '😨', '😰', '😥', '😓', '😔', '😕', '😖', '😣', '😞', '😟', '😤', '😠', '😱', '😲', '😳', '🥵', '🥶', '😵', '😷', '🤒', '🤕', '🤢', '🤮', '🤤', '😇', '🥰', '😍', '🤩', '😘', '😗', '☺️', '😚', '😙', '🥲', '🙃', '😈', '🤡', '🤠', '🥳', '🥴', '🥱', '🤭', '🤫', '🤪', '🤴', '🥸', '🤐', '🤨', '🤧', '🤥', '🤬', '😪', '😫', '😩', '😵‍💫', '🥺', '😏', '😶‍🌫️'],
  '동물': ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐻‍❄️', '🐨', '🐯', '🦁', '🐮', '🐷', '🐸', '🐵', '🙈', '🙉', '🙊', '🐒', '🐔', '🐧', '🐦', '🐤', '🐣', '🐥', '🦆', '🦅', '🦉', '🦇', '🐺', '🐗', '🐴', '🦄', '🐝', '🐛', '🦋', '🐌', '🐞', '🐜', '🦗', '🐍', '🦕', '🐢', '🦎', '🦖', '🦕', '🐙', '🐠', '🐟', '🐡', '🦈', '🐳', '🐋', '🐬', '🦭', '🐈', '🐈‍⬛', '🦁', '🐅', '🐆', '🐴', '🦓', '🦌', '🦍', '🐘', '🦏', '🦛', '🐪', '🐫', '🦒', '🦘', '🦙', '🐃', '🐂', '🐄', '🐎', '🐖', '🐐', '🐑', '🐕', '🐕‍🦺', '🐩', '🐈', '🐓', '🦃', '🕊', '🐇', '🐁', '🐀', '🐿️', '🦫', '🐾'],
  '음식': ['🍎', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🫐', '🍈', '🍒', '🥝', '🍑', '🥭', '🍍', '🥥', '🍅', '🍆', '🥒', '🥕', '🌽', '🌶️', '🫑', '🥔', '🍠', '🌰', '🥬', '🥦', '🧄', '🧅', '🍄', '🌵', '🌴', '🌲', '🌳', '🌿', '☘️', '🍀', '🍁', '🍂', '🍃', '🌾', '🌺', '🌻', '🌹', '🥀', '🌷', '🌼', '🌸', '🌿', '🍞', '🥖', '🥨', '🥯', '🧀', '🍖', '🍗', '🥩', '🍟', '🍔', '🍝', '🍕', '🌭', '🥪', '🌮', '🌯', '🥙', '🥘', '🥚', '🍳', '🥞', '🧈', '🍲', '🍜', '🍛', '🍣', '🍱', '🍚', '🍙', '🍘', '🍥', '🥮', '🍢', '🍡', '🍧', '🍨', '🍦', '🥧', '🍰', '🎂', '🧁', '🍯', '🍮', '🍭', '🍬', '🍫', '🍿', '🍩', '🍪', '☕', '🍵', '🫖', '🍶', '🍼', '🍺', '🍻', '🥂', '🍷', '🥃', '🍸', '🍹', '🍾', '🥤', '🥛', '🥜'],
  '활동': ['⚽', '🏀', '🏈', '⚾', '🥎', '🏓', '🏸', '🏒', '🏑', '🏏', '🥍', '🏐', '⛳', '🏹', '🎣', '🥊', '🥋', '🥅', '⛷️', '🏂', '🏄', '🏄‍♂️', '🏄‍♀️', '🚣', '🚣‍♂️', '🚣‍♀️', '🏊', '🏊‍♂️', '🏊‍♀️', '⛹️', '⛹️‍♂️', '⛹️‍♀️', '🏋️', '🏋️‍♂️', '🏋️‍♀️', '🚴', '🚴‍♂️', '🚴‍♀️', '🚵', '🚵‍♂️', '🚵‍♀️', '🏎️', '🏍️', '🤸', '🤸‍♂️', '🤸‍♀️', '🤼', '🤼‍♂️', '🤼‍♀️', '🤽', '🤽‍♂️', '🤽‍♀️', '🤾', '🤾‍♂️', '🤾‍♀️', '🤹', '🤹‍♂️', '🤹‍♀️'],
  '기타': ['❤️', '💜', '💙', '💚', '💛', '🧡', '🔥', '✨', '💫', '⚡', '🎉', '🎊', '👏', '🙌', '💪', '👍', '👎', '👊', '✊', '🤝', '🙏', '✍️', '💬', '👁️‍🗨️', '🗨️', '💭', '💯', '💢', '💥', '💦', '💨', '🕳️', '💵', '💴', '💶', '💷', '💰', '💳', '💎', '⚖️', '🧨', '🔧', '🔨', '⚒️', '🛠️', '⚔️', '🔫', '🏹', '🛡️', '🔪', '⚙️', '🧿', '⛑️', '🚨', '🚩', '🏁', '🏳️', '🏴', '🏳️‍🌈', '🏳️‍⚧️', '🏴‍☠️']
};

export default function EmojiPicker({ onEmojiSelect, className = '' }: Props) {
  const [isOpen, setIsOpen] = useState(false);
  const [activeCategory, setActiveCategory] = useState('우주');

  const handleEmojiClick = (emoji: string) => {
    onEmojiSelect(emoji);
    // 창을 닫지 않고 유지
  };

  return (
    <div className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="p-2 text-yellow-400 hover:text-yellow-300 hover:bg-gray-700/50 rounded-md transition-colors"
        title="이모티콘 추가"
      >
        😊
      </button>

      {isOpen && (
        <div className="absolute bottom-full mb-2 left-0 sm:right-0 sm:left-auto bg-gray-800 border border-gray-600 rounded-lg shadow-xl z-[9999] w-80 max-w-[90vw]">
          {/* 카테고리 탭 */}
          <div className="flex border-b border-gray-600">
            {Object.keys(EMOJI_CATEGORIES).map((category) => (
              <button
                key={category}
                type="button"
                onClick={() => setActiveCategory(category)}
                className={`flex-1 px-3 py-2 text-xs font-medium transition-colors ${
                  activeCategory === category
                    ? 'bg-purple-600 text-white'
                    : 'text-gray-300 hover:bg-gray-700'
                }`}
              >
                {category}
              </button>
            ))}
          </div>

          {/* 이모티콘 그리드 */}
          <div className="p-3 max-h-48 overflow-y-auto">
            <div className="grid grid-cols-5 gap-2">
              {(EMOJI_CATEGORIES[activeCategory] || []).map((emoji, index) => (
                <button
                  key={index}
                  type="button"
                  onClick={() => handleEmojiClick(emoji)}
                  className="p-2 text-lg hover:bg-gray-700 rounded transition-colors"
                  title={emoji}
                >
                  {emoji}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 클릭 외부 영역 감지 */}
      {isOpen && (
        <div
          className="fixed inset-0 z-[9998]"
          onClick={() => setIsOpen(false)}
        />
      )}
    </div>
  );
}