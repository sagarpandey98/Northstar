import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

// Toast component for notifications
const Toast = ({ message, type, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 3000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className={`fixed top-4 right-4 p-4 rounded-lg shadow-lg z-50 ${
      type === 'error' ? 'bg-red-500 text-white' : 'bg-green-500 text-white'
    }`}>
      <div className="flex items-center justify-between">
        <span>{message}</span>
        <button onClick={onClose} className="ml-4 text-white hover:text-gray-200">
          ×
        </button>
      </div>
    </div>
  );
};

// Confirmation Modal
const ConfirmationModal = ({ isOpen, title, message, onConfirm, onCancel, onSave }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
        <h3 className="text-lg font-semibold mb-4">{title}</h3>
        <p className="text-gray-600 mb-6">{message}</p>
        <div className="flex gap-3 justify-end">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-gray-600 border border-gray-300 rounded hover:bg-gray-50"
          >
            Cancel
          </button>
          {onSave && (
            <button
              onClick={onSave}
              className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
            >
              Save
            </button>
          )}
          <button
            onClick={onConfirm}
            className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
          >
            Discard
          </button>
        </div>
      </div>
    </div>
  );
};

// Edit Modal for adding/editing items
const EditModal = ({ isOpen, item, level, onSave, onClose, title }) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    if (isOpen) {
      setName(item?.name || '');
      setDescription(item?.description || '');
    }
  }, [isOpen, item]);

  const handleSave = () => {
    if (!name.trim()) return;
    onSave({ name: name.trim(), description: description.trim() });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
        <h3 className="text-lg font-semibold mb-4">{title}</h3>
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Name *
          </label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full p-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Enter name"
          />
        </div>
        <div className="mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Description
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full p-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            rows="3"
            placeholder="Enter description"
          />
        </div>
        <div className="flex gap-3 justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-600 border border-gray-300 rounded hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={!name.trim()}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:bg-gray-300"
          >
            Save
          </button>
        </div>
      </div>
    </div>
  );
};

// Tree Node Component
const TreeNode = ({ 
  item, 
  level, 
  onEdit, 
  onDelete, 
  onAddChild, 
  path 
}) => {
  const isNew = !item.uuid;
  const canAddChild = level < 2; // Max 3 levels (0, 1, 2)
  
  const levelNames = ['Domain', 'Sub Domain', 'Specific'];
  const levelColors = {
    0: 'bg-blue-50 border-blue-200',
    1: 'bg-green-50 border-green-200', 
    2: 'bg-purple-50 border-purple-200'
  };

  return (
    <div className={`border rounded-lg p-4 mb-3 ${levelColors[level]}`}>
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <span className="text-xs px-2 py-1 bg-gray-200 rounded">
            {levelNames[level]}
          </span>
          {isNew && (
            <span className="text-xs px-2 py-1 bg-yellow-200 text-yellow-800 rounded">
              NEW
            </span>
          )}
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => onEdit(item, path)}
            className="text-xs px-2 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            Edit
          </button>
          <button
            onClick={() => onDelete(path)}
            className="text-xs px-2 py-1 bg-red-500 text-white rounded hover:bg-red-600"
          >
            Delete
          </button>
          {canAddChild && (
            <button
              onClick={() => onAddChild(path, level + 1)}
              className="text-xs px-2 py-1 bg-green-500 text-white rounded hover:bg-green-600"
            >
              Add {levelNames[level + 1]}
            </button>
          )}
        </div>
      </div>
      
      <h4 className="font-semibold text-gray-800">{item.name}</h4>
      {item.description && (
        <p className="text-sm text-gray-600 mt-1">{item.description}</p>
      )}
      
      {/* Render children */}
      {level === 0 && item.subDomains && item.subDomains.length > 0 && (
        <div className="mt-4 ml-4">
          {item.subDomains.map((subDomain, index) => (
            <TreeNode
              key={subDomain.uuid || `new-subdomain-${index}`}
              item={subDomain}
              level={1}
              onEdit={onEdit}
              onDelete={onDelete}
              onAddChild={onAddChild}
              path={[...path, 'subDomains', index]}
            />
          ))}
        </div>
      )}
      
      {level === 1 && item.specifics && item.specifics.length > 0 && (
        <div className="mt-4 ml-4">
          {item.specifics.map((specific, index) => (
            <TreeNode
              key={specific.uuid || `new-specific-${index}`}
              item={specific}
              level={2}
              onEdit={onEdit}
              onDelete={onDelete}
              onAddChild={onAddChild}
              path={[...path, 'specifics', index]}
            />
          ))}
        </div>
      )}
    </div>
  );
};

// Main Domain Management Component
const DomainManagement = () => {
  const [categories, setCategories] = useState(null);
  const [originalCategories, setOriginalCategories] = useState(null);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);
  const [editModal, setEditModal] = useState({ isOpen: false });
  const [confirmModal, setConfirmModal] = useState({ isOpen: false });
  
  const navigate = useNavigate();
  const location = useLocation();

  // Fetch categories from API
  const fetchCategories = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/v1/categories', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) throw new Error('Failed to fetch categories');
      
      const data = await response.json();
      setCategories(data);
      setOriginalCategories(JSON.parse(JSON.stringify(data))); // Deep copy
      setHasUnsavedChanges(false);
    } catch (error) {
      setToast({ message: 'Failed to load categories', type: 'error' });
    } finally {
      setLoading(false);
    }
  }, []);

  // Load data on component mount
  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  // Navigation guard
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = '';
      }
    };

    const handlePopState = (e) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
        setConfirmModal({
          isOpen: true,
          title: 'Unsaved Changes',
          message: 'You have unsaved changes. Do you want to save before leaving?',
          onConfirm: () => {
            setConfirmModal({ isOpen: false });
            setHasUnsavedChanges(false);
            window.history.go(-1);
          },
          onSave: () => handleSave(true),
          onCancel: () => setConfirmModal({ isOpen: false })
        });
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    window.addEventListener('popstate', handlePopState);
    
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      window.removeEventListener('popstate', handlePopState);
    };
  }, [hasUnsavedChanges]);

  // Helper function to get nested object by path
  const getByPath = (obj, path) => {
    return path.reduce((current, key) => current?.[key], obj);
  };

  // Helper function to set nested object by path
  const setByPath = (obj, path, value) => {
    const newObj = JSON.parse(JSON.stringify(obj));
    let current = newObj;
    
    for (let i = 0; i < path.length - 1; i++) {
      current = current[path[i]];
    }
    
    if (value === null) {
      // Delete operation
      if (Array.isArray(current[path[path.length - 1]])) {
        current[path[path.length - 1]].splice(path[path.length], 1);
      } else {
        delete current[path[path.length - 1]];
      }
    } else {
      current[path[path.length - 1]] = value;
    }
    
    return newObj;
  };

  // Add new item
  const handleAddChild = (parentPath, level) => {
    if (level > 2) {
      setToast({ 
        message: 'Cannot add children under Specific level. Maximum 3 levels allowed.', 
        type: 'error' 
      });
      return;
    }

    setEditModal({
      isOpen: true,
      title: `Add New ${['Domain', 'Sub Domain', 'Specific'][level]}`,
      level,
      parentPath,
      onSave: (data) => {
        const newItem = {
          name: data.name,
          description: data.description,
          // No uuid for new items - backend will create
        };

        // Add appropriate children arrays
        if (level === 0) newItem.subDomains = [];
        if (level === 1) newItem.specifics = [];

        let newCategories;
        if (level === 0) {
          // Adding new domain
          newCategories = {
            ...categories,
            domains: [...categories.domains, newItem]
          };
        } else {
          // Adding to existing parent
          const parent = getByPath(categories, parentPath);
          const childKey = level === 1 ? 'subDomains' : 'specifics';
          const updatedParent = {
            ...parent,
            [childKey]: [...(parent[childKey] || []), newItem]
          };
          newCategories = setByPath(categories, parentPath, updatedParent);
        }

        setCategories(newCategories);
        setHasUnsavedChanges(true);
        setEditModal({ isOpen: false });
        setToast({ message: 'Item added successfully', type: 'success' });
      },
      onClose: () => setEditModal({ isOpen: false })
    });
  };

  // Edit existing item
  const handleEdit = (item, path) => {
    setEditModal({
      isOpen: true,
      title: `Edit ${item.name}`,
      item,
      path,
      onSave: (data) => {
        const updatedItem = { ...item, ...data };
        const newCategories = setByPath(categories, path, updatedItem);
        setCategories(newCategories);
        setHasUnsavedChanges(true);
        setEditModal({ isOpen: false });
        setToast({ message: 'Item updated successfully', type: 'success' });
      },
      onClose: () => setEditModal({ isOpen: false })
    });
  };

  // Delete item
  const handleDelete = (path) => {
    const newCategories = setByPath(categories, path, null);
    setCategories(newCategories);
    setHasUnsavedChanges(true);
    setToast({ message: 'Item deleted successfully', type: 'success' });
  };

  // Save changes to backend
  const handleSave = async (shouldNavigate = false) => {
    try {
      setSaving(true);
      const response = await fetch('/api/v1/categories', {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(categories),
      });

      if (!response.ok) throw new Error('Failed to save categories');

      const updatedData = await response.json();
      setCategories(updatedData);
      setOriginalCategories(JSON.parse(JSON.stringify(updatedData)));
      setHasUnsavedChanges(false);
      setToast({ message: 'Changes saved successfully', type: 'success' });
      
      if (shouldNavigate) {
        setConfirmModal({ isOpen: false });
        window.history.go(-1);
      }
    } catch (error) {
      setToast({ message: 'Failed to save changes', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  // Reset changes
  const handleReset = () => {
    setCategories(JSON.parse(JSON.stringify(originalCategories)));
    setHasUnsavedChanges(false);
    setToast({ message: 'Changes reset successfully', type: 'success' });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Loading categories...</div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Domain Management</h1>
          <p className="text-gray-600 mt-1">
            Manage your category structure (Domain → Sub Domain → Specific)
          </p>
        </div>
        
        <div className="flex gap-3">
          {hasUnsavedChanges && (
            <button
              onClick={handleReset}
              className="px-4 py-2 text-gray-600 border border-gray-300 rounded hover:bg-gray-50"
            >
              Reset Changes
            </button>
          )}
          <button
            onClick={() => handleAddChild([], 0)}
            className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
          >
            Add Domain
          </button>
          <button
            onClick={() => handleSave()}
            disabled={!hasUnsavedChanges || saving}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:bg-gray-300"
          >
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </div>

      {/* Unsaved changes indicator */}
      {hasUnsavedChanges && (
        <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
          <div className="flex items-center">
            <div className="w-2 h-2 bg-yellow-500 rounded-full mr-2"></div>
            <span className="text-yellow-800 text-sm">
              You have unsaved changes
            </span>
          </div>
        </div>
      )}

      {/* Category Tree */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        {categories?.domains && categories.domains.length > 0 ? (
          <div className="space-y-4">
            {categories.domains.map((domain, index) => (
              <TreeNode
                key={domain.uuid || `new-domain-${index}`}
                item={domain}
                level={0}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onAddChild={handleAddChild}
                path={['domains', index]}
              />
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <div className="text-gray-500 mb-4">No domains found</div>
            <button
              onClick={() => handleAddChild([], 0)}
              className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
            >
              Add Your First Domain
            </button>
          </div>
        )}
      </div>

      {/* Modals */}
      <EditModal {...editModal} />
      <ConfirmationModal {...confirmModal} />
      
      {/* Toast */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
};

export default DomainManagement;
