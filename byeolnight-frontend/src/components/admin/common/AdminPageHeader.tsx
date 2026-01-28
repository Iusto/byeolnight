interface AdminPageHeaderProps {
  title: string;
  description?: string;
}

export default function AdminPageHeader({ title, description }: AdminPageHeaderProps) {
  return (
    <div>
      <h1 className="text-2xl font-bold text-white">{title}</h1>
      {description && <p className="text-gray-400 mt-1">{description}</p>}
    </div>
  );
}