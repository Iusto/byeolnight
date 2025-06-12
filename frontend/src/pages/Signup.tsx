const Signup = () => {
  return (
    <div className="flex justify-center items-center min-h-screen bg-gray-50">
      <div className="w-full max-w-md bg-white p-6 rounded shadow">
        <h2 className="text-2xl font-bold mb-6 text-center">회원가입</h2>
        <form>
          <input type="email" placeholder="이메일" className="w-full mb-4 p-2 border rounded" />
          <input type="text" placeholder="닉네임" className="w-full mb-4 p-2 border rounded" />
          <input type="password" placeholder="비밀번호" className="w-full mb-4 p-2 border rounded" />
          <input type="password" placeholder="비밀번호 확인" className="w-full mb-4 p-2 border rounded" />
          <button className="w-full bg-green-500 text-white py-2 rounded hover:bg-green-600">
            회원가입
          </button>
        </form>
      </div>
    </div>
  )
}

export default Signup
