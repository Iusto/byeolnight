export interface TuiEditorInstance {
  exec: (command: string, payload: Record<string, string>) => void;
  getMarkdown: () => string;
  getHTML: () => string;
  insertText: (content: string) => void;
  setMarkdown: (content: string) => void;
}

export interface TuiEditorHandle {
  getInstance: () => TuiEditorInstance | undefined;
  insertContent: (content: string) => void;
  getMarkdown: () => string;
  getHTML: () => string;
}
