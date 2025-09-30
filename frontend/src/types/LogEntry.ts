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
//   parsingError: boolean;
//   parsingErrorMessage?: string;
//   lineNumber: number;
//   logFileId?: string;
}

export interface ParsingStats {
  totalEntries: number;
  planSectionEntries: number;
  applySectionEntries: number;
  errorEntries: number;
  warnEntries: number;
}

export interface LogUploadResponse {
  logFileId: string;
  entriesCount: number;
  errorsCount: number;
  stats: ParsingStats;
  entries?: LogEntry[];
}

export interface LogResponse {
  entries: LogEntry[];
  stats: ParsingStats;
}