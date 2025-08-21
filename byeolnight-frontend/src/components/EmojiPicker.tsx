import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';

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
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const buttonRef = useRef<HTMLButtonElement>(null);
  
  const isChat = className?.includes('border-l-0');

  const handleEmojiClick = (emoji: string) => {
    onEmojiSelect(emoji);
  };

  useEffect(() => {
    if (isOpen && buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      const isMobile = typeof window !== 'undefined' && window.innerWidth < 768;
      
      if (isChat && !isMobile) {
        setPosition({
          top: rect.top,
          left: rect.left - 288 - 8
        });
      } else {
        setPosition({
          top: rect.bottom + 8,
          left: rect.left
        });
      }
    }
  }, [isOpen, isChat]);

  const renderModal = () => (
    <div 
      className="fixed w-72 max-w-[calc(100vw-2rem)] bg-gray-800 border border-gray-600 rounded-lg shadow-xl z-[100000]"
      style={{
        top: `${position.top}px`,
        left: `${Math.max(8, position.left)}px`
      }}
    >
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
  );

  return (
    <div className={className}>
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="p-2 text-yellow-400 hover:text-yellow-300 hover:bg-gray-700/50 rounded-md transition-colors"
        title="이모티콘 추가"
      >
        😊
      </button>

      {isOpen && typeof document !== 'undefined' && createPortal(
        <>
          <div
            className="fixed inset-0 z-[99999]"
            onClick={() => setIsOpen(false)}
          />
          {renderModal()}
        </>,
        document.body
      )}
    </div>
  );
}