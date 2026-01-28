import { Link } from 'react-router-dom';

interface AdminStatsCardProps {
  title: string;
  value: number | string;
  subValue?: string;
  icon?: string;
  color?: 'purple' | 'green' | 'red' | 'cyan' | 'orange' | 'yellow' | 'blue';
  link?: string;
  alert?: boolean;
}

const colorClasses: Record<string, { border: string; text: string; gradient: string }> = {
  purple: {
    border: 'border-purple-500/20',
    text: 'text-purple-400',
    gradient: 'from-purple-500 to-purple-600',
  },
  green: {
    border: 'border-green-500/20',
    text: 'text-green-400',
    gradient: 'from-green-500 to-green-600',
  },
  red: {
    border: 'border-red-500/20',
    text: 'text-red-400',
    gradient: 'from-red-500 to-red-600',
  },
  cyan: {
    border: 'border-cyan-500/20',
    text: 'text-cyan-400',
    gradient: 'from-cyan-500 to-cyan-600',
  },
  orange: {
    border: 'border-orange-500/20',
    text: 'text-orange-400',
    gradient: 'from-orange-500 to-orange-600',
  },
  yellow: {
    border: 'border-yellow-500/20',
    text: 'text-yellow-400',
    gradient: 'from-yellow-500 to-yellow-600',
  },
  blue: {
    border: 'border-blue-500/20',
    text: 'text-blue-400',
    gradient: 'from-blue-500 to-blue-600',
  },
};

export default function AdminStatsCard({
  title,
  value,
  subValue,
  icon,
  color = 'purple',
  link,
  alert,
}: AdminStatsCardProps) {
  const colors = colorClasses[color];

  const content = (
    <div className={`relative bg-[#1f2336]/80 backdrop-blur-md rounded-xl p-4 border ${colors.border} transition-all duration-200 ${link ? 'hover:border-opacity-50 cursor-pointer' : ''}`}>
      {alert && (
        <span className="absolute top-3 right-3 w-3 h-3 bg-red-500 rounded-full animate-pulse" />
      )}
      {icon && (
        <div className={`inline-flex p-2 rounded-lg bg-gradient-to-br ${colors.gradient} mb-2`}>
          <span className="text-xl">{icon}</span>
        </div>
      )}
      <p className="text-gray-400 text-sm">{title}</p>
      <p className={`text-2xl font-bold ${colors.text}`}>{value}</p>
      {subValue && <p className="text-gray-500 text-xs mt-1">{subValue}</p>}
    </div>
  );

  if (link) {
    return <Link to={link}>{content}</Link>;
  }

  return content;
}