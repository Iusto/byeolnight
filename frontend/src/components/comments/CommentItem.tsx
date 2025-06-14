export default function CommentItem({ comment, onReplySubmit }: Props) {
  const [showReply, setShowReply] = useState(false);
  const [replyContent, setReplyContent] = useState("");

  const handleReply = () => {
    if (!replyContent.trim()) return;
    onReplySubmit(replyContent, comment.id);
    setReplyContent("");
    setShowReply(false);
  };

  return (
    <div className="ml-2 mt-2 border p-2 rounded">
      <div>
        <strong>{comment.writer}</strong>: {comment.content}
      </div>
      <button onClick={() => setShowReply(!showReply)} className="text-sm text-blue-500">
        답글
      </button>

      {showReply && (
        <div className="mt-1">
          <textarea
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
            className="w-full border p-1 mt-1"
            placeholder="답글을 입력하세요"
          />
          <button
            onClick={handleReply}
            className="bg-blue-500 text-white px-2 py-1 rounded mt-1"
          >
            등록
          </button>
        </div>
      )}
    </div>
  );
}
