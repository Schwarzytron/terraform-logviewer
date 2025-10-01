export enum LogLevel {
  ERROR = 'ERROR',
  WARN = 'WARN',
  INFO = 'INFO',
  DEBUG = 'DEBUG'
}

export interface LogEntry {
  id: number;
  rawMessage: string;
  timestamp: string;
  level: LogLevel;
  section: string;
  message: string;
  hasJson: boolean;
  parsingError: boolean;
  parsingErrorMessage?: string;
  lineNumber: number;
  logFileId: string;
  tfResourceType?: string;
  tfReqId?: string;
  isRead: boolean;
  requestType?: string;
  jsonBody?: string;
}

export interface ParsingStats {
  totalEntries: number;
  planSectionEntries: number;
  applySectionEntries: number;
  errorEntries: number;
  warnEntries: number;
}

export interface LogResponse {
  logFileId: string;
  entriesProcessed: number;
  errorsCount: number;
  stats: Record<string, any>;
  entries: LogEntry[];
}

export interface SearchFilters {
  tfResourceType?: string;
  timestampFrom?: string;
  timestampTo?: string;
  level?: LogLevel;
  section?: string;
  tfReqId?: string;
  onlyUnread?: boolean;
  freeText?: string;
  page?: number;
  size?: number;
}