import axios from "axios";
import type { Comment } from "@/types/comment";

const BASE_URL = "/api/comments";
export const __comment_helper__ = null;

export const fetchComments = async (postId: number): Promise<Comment[]> => {
  const res = await axios.get(`${BASE_URL}/post/${postId}`);
  return res.data.data;
};

export const submitComment = async (payload: {
  postId: number;
  content: string;
  parentId?: number;
}): Promise<number> => {
  const res = await axios.post(BASE_URL, payload);
  return res.data.data;
};
