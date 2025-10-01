import { Button } from "react-bootstrap";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

const Pagination: React.FC<PaginationProps> = ({ currentPage, totalPages, onPageChange }) => {
  return (
    <div className="d-flex justify-content-between align-items-center">
      <div>
        Page {currentPage + 1} of {totalPages}
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