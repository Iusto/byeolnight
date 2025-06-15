// src/api/posts.ts
import axios from 'axios';
import { Post } from '../types/Post';

const PUBLIC_BASE_URL = '/api/public/posts';
const MEMBER_BASE_URL = '/api/member/posts';

/**
 * 게시글 목록 조회 (공용)
 */
export const fetchPosts = async (): Promise<Post[]> => {
  const response = await axios.get(`${PUBLIC_BASE_URL}`);
  return response.data.data.content;
};

/**
 * 게시글 상세 조회 (공용)
 */
export const fetchPostById = async (id: number): Promise<Post> => {
  const response = await axios.get(`${PUBLIC_BASE_URL}/${id}`);
  return response.data.data;
};

/**
 * 게시글 작성 (회원 전용)
 */
export const createPost = async (
  title: string,
  content: string,
  category: string,
  token: string
): Promise<number> => {
  const response = await axios.post(
    MEMBER_BASE_URL,
    { title, content, category },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
  return response.data.data;
};

/**
 * 게시글 수정 (회원 전용)
 */
export const updatePost = async (
  id: number,
  title: string,
  content: string,
  category: string,
  token: string
): Promise<void> => {
  await axios.put(
    `${MEMBER_BASE_URL}/${id}`,
    { title, content, category },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
};

/**
 * 게시글 삭제 (회원 전용)
 */
export const deletePost = async (id: number, token: string): Promise<void> => {
  await axios.delete(`${MEMBER_BASE_URL}/${id}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
};
