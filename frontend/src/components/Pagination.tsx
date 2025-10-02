import React from 'react';
import { Button } from "react-bootstrap";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}

const Pagination: React.FC<PaginationProps> = ({ currentPage, totalPages, totalElements, onPageChange }) => {
  return (
    <div className="d-flex justify-content-between align-items-center mt-3">
      <div className="text-muted">
        Showing page {currentPage + 1} of {totalPages} ({totalElements} total entries)
      </div>
      <div>
        <Button
          variant="outline-primary"
          size="sm"
          disabled={currentPage === 0}
          onClick={() => onPageChange(currentPage - 1)}
        >
          Previous
        </Button>
        <span className="mx-2">|</span>
        <Button
          variant="outline-primary"
          size="sm"
          disabled={currentPage >= totalPages - 1}
          onClick={() => onPageChange(currentPage + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  );
};