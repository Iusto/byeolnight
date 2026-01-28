interface AdminStatusBadgeProps {
  status: string;
  variant?: 'status' | 'role' | 'type';
}

const statusColors: Record<string, string> = {
  // User status
  ACTIVE: 'bg-green-600 text-white',
  SUSPENDED: 'bg-yellow-600 text-black',
  BANNED: 'bg-red-600 text-white',
  WITHDRAWN: 'bg-gray-600 text-gray-300',
  // Role
  ADMIN: 'bg-red-600 text-white',
  USER: 'bg-gray-600 text-gray-300',
  // Post/Comment status
  BLINDED: 'bg-yellow-600 text-black',
  DELETED: 'bg-gray-600 text-gray-300',
  REPORTED: 'bg-orange-600 text-white',
  // Default
  default: 'bg-gray-600 text-gray-300',
};

const typeColors: Record<string, string> = {
  GOOGLE: 'bg-cyan-600 text-white',
  KAKAO: 'bg-yellow-500 text-black',
  NAVER: 'bg-green-600 text-white',
  REGULAR: 'bg-orange-600 text-white',
  default: 'bg-gray-600 text-gray-300',
};

const typeLabels: Record<string, string> = {
  GOOGLE: '구글',
  KAKAO: '카카오',
  NAVER: '네이버',
  REGULAR: '일반',
};

export default function AdminStatusBadge({ status, variant = 'status' }: AdminStatusBadgeProps) {
  const getColorClass = () => {
    if (variant === 'type') {
      return typeColors[status] || typeColors.default;
    }
    return statusColors[status] || statusColors.default;
  };

  const getLabel = () => {
    if (variant === 'type') {
      return typeLabels[status] || status;
    }
    return status;
  };

  return (
    <span className={`text-xs px-2 py-1 rounded-full font-medium ${getColorClass()}`}>
      {getLabel()}
    </span>
  );
}