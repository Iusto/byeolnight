import axiosInstance from "@/lib/axiosInstance";
import type { PostDetail } from "@/types/post";

export const fetchPostDetail = async (postId: number): Promise<PostDetail> => {
  const res = await axiosInstance.get(`/public/posts/${postId}`);
  return res.data.data;
};
