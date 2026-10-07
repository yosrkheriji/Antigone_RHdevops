import React, { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { HiOutlineX } from 'react-icons/hi';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  size?: 'sm' | 'md' | 'lg';
}

const sizeClasses = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
};

const Modal: React.FC<ModalProps> = ({ isOpen, onClose, title, children, footer, size = 'md' }) => {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return createPortal(
    <div className="fixed inset-0 z-[100] flex items-center justify-center">
      <div className="absolute inset-0 bg-gray-900/50" onClick={onClose} />
      <div
        className={`relative ${sizeClasses[size]} mx-4 w-full max-h-[88vh] flex flex-col rounded-2xl border border-gray-200 bg-white shadow-theme-lg overflow-hidden dark:border-gray-700 dark:bg-gray-800`}
      >
        <div className="flex items-center justify-between border-b border-gray-200 p-5 dark:border-gray-700 shrink-0">
          <h3 className="text-lg font-semibold text-gray-800 dark:text-white">{title}</h3>
          <button
            onClick={onClose}
            className="rounded-lg p-1 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <HiOutlineX size={20} className="text-gray-500 dark:text-gray-400" />
          </button>
        </div>
        <div className="p-5 overflow-y-auto flex-1">{children}</div>
        {footer && (
          <div className="shrink-0 flex justify-end gap-3 px-5 py-4 border-t border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800">
            {footer}
          </div>
        )}
      </div>
    </div>,
    document.body
  );
};

export default Modal;
