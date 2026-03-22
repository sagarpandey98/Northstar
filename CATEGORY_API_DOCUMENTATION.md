# Category Management API Documentation

## 📋 Overview

The Category Management API provides a sophisticated system for managing hierarchical categories with **automatic change detection**. The frontend sends the complete category structure, and the backend intelligently determines what needs to be created, updated, or deleted.

## 🏗️ Category Structure

The system supports a 3-level hierarchy:
```
Domain → SubDomain → Specific
```

Each level has:
- `name`: Display name
- `uuid`: Unique identifier (empty for new items)
- `description`: Optional description

## 🔗 Base URL
```
http://localhost:8081/api/v1/categories
```

---

## 📚 API Endpoints

### 1. Get Category Structure
**Endpoint:** `GET /api/v1/categories/{userId}`

**Description:** Retrieves the complete category structure for a specific user.

**Parameters:**
- `userId` (path): User identifier

**Response:**
```json
{
  "name": "User Categories",
  "uuid": "user-123",
  "domains": [
    {
      "name": "Health",
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "description": "Health and wellness activities",
      "subDomains": [
        {
          "name": "Fitness",
          "uuid": "550e8400-e29b-41d4-a716-446655440002",
          "description": "Physical fitness activities",
          "specifics": [
            {
              "name": "Cardio",
              "uuid": "550e8400-e29b-41d4-a716-446655440003",
              "description": "Cardiovascular exercises"
            },
            {
              "name": "Strength Training",
              "uuid": "550e8400-e29b-41d4-a716-446655440004",
              "description": "Weight and resistance training"
            }
          ]
        }
      ]
    }
  ]
}
```

**Example Request:**
```javascript
fetch('http://localhost:8081/api/v1/categories/user-123')
  .then(response => response.json())
  .then(data => console.log(data));
```

---

### 2. Update Category Structure
**Endpoint:** `PUT /api/v1/categories`

**Description:** Updates the complete category structure. The backend automatically detects what needs to be created, updated, or deleted.

### 3. Patch Category Structure
**Endpoint:** `PATCH /api/v1/categories`

**Description:** Partial update of category structure. Same functionality as PUT but semantically represents partial updates. Also returns the complete structure after changes.

### 4. Create/Update Category Structure (Alternative)
**Endpoint:** `POST /api/v1/categories`

**Description:** Same functionality as PUT endpoint, follows REST convention for create-or-update operations.

**Request Body (same for PUT, PATCH, POST):**
```json
{
  "userName": "Sagar Pandey",
  "uuid": "user-123",
  "domains": [
    {
      "name": "Health & Wellness",
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "description": "Updated description",
      "subDomains": [
        {
          "name": "Fitness",
          "uuid": "550e8400-e29b-41d4-a716-446655440002",
          "description": "Physical fitness activities",
          "specifics": [
            {
              "name": "Cardio",
              "uuid": "550e8400-e29b-41d4-a716-446655440003",
              "description": "Cardiovascular exercises"
            },
            {
              "name": "Yoga",
              "uuid": "",
              "description": "Yoga and stretching"
            }
          ]
        },
        {
          "name": "Nutrition",
          "uuid": "",
          "description": "Diet and nutrition tracking",
          "specifics": []
        }
      ]
    }
  ]
}
```

**Response (same for all methods):** Same format as GET endpoint with updated data

**Example Request (works for PUT, PATCH, POST):**
```javascript
const updatedCategories = {
  userName: "Sagar Pandey",
  uuid: "user-123",
  domains: [
    // ... your updated structure
  ]
};

fetch('http://localhost:8081/api/v1/categories', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify(updatedCategories)
})
.then(response => response.json())
.then(data => console.log('Updated:', data));
```

---

### 3. Patch Category Structure
**Endpoint:** `PATCH /api/v1/categories`

**Description:** Partial update of category structure. Same functionality as PUT but semantically represents partial updates. Also returns the complete structure after changes.

**Request/Response:** Same as PUT endpoint

---

### 4. Create/Update Category Structure (Alternative)
**Endpoint:** `POST /api/v1/categories`

**Description:** Same functionality as PUT endpoint, follows REST convention for create-or-update operations.

**Request/Response:** Same as PUT endpoint

---

## 🤖 Automatic Change Detection

The backend automatically detects changes by comparing the received data with existing database records:

### ✅ CREATE Detection
- **Condition:** `uuid` is empty (`""`) or missing
- **Action:** Generate new UUID and create record

**Example:**
```json
{
  "name": "New Category",
  "uuid": "",           // Empty UUID = CREATE
  "description": "This will be created"
}
```

### 🔄 UPDATE Detection
- **Condition:** `uuid` exists in database but `name` or `description` changed
- **Action:** Update existing record

**Example:**
```json
{
  "name": "Updated Name",        // Changed from original
  "uuid": "existing-uuid-123",   // Existing UUID = UPDATE
  "description": "Updated description"
}
```

### ❌ DELETE Detection
- **Condition:** Item exists in database but missing from request
- **Action:** Delete record and all its children

**Example:** If "Strength Training" existed before but is not in the new request, it will be deleted.

---

## 📝 Frontend Integration Examples

### React/JavaScript Implementation

```javascript
class CategoryManager {
  constructor(baseUrl = 'http://localhost:8081/api/v1/categories') {
    this.baseUrl = baseUrl;
  }

  // Get categories for user
  async getCategories(userId) {
    try {
      const response = await fetch(`${this.baseUrl}/${userId}`);
      return await response.json();
    } catch (error) {
      console.error('Error fetching categories:', error);
      throw error;
    }
  }

  // Update complete category structure
  async updateCategories(categoryData) {
    try {
      const response = await fetch(this.baseUrl, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(categoryData)
      });
      return await response.json();
    } catch (error) {
      console.error('Error updating categories:', error);
      throw error;
    }
  }

  // Patch category structure (partial update)
  async patchCategories(categoryData) {
    try {
      const response = await fetch(this.baseUrl, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(categoryData)
      });
      return await response.json();
    } catch (error) {
      console.error('Error patching categories:', error);
      throw error;
    }
  }

  // Create/Update category structure (alternative)
  async createOrUpdateCategories(categoryData) {
    try {
      const response = await fetch(this.baseUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(categoryData)
      });
      return await response.json();
    } catch (error) {
      console.error('Error creating/updating categories:', error);
      throw error;
    }
  }

  // Helper: Add new domain
  addNewDomain(categories, domainName, description = '') {
    const newDomain = {
      name: domainName,
      uuid: '', // Empty UUID for creation
      description: description,
      subDomains: []
    };
    categories.domains.push(newDomain);
    return categories;
  }

  // Helper: Add new subdomain to existing domain
  addNewSubDomain(categories, domainUuid, subDomainName, description = '') {
    const domain = categories.domains.find(d => d.uuid === domainUuid);
    if (domain) {
      const newSubDomain = {
        name: subDomainName,
        uuid: '', // Empty UUID for creation
        description: description,
        specifics: []
      };
      domain.subDomains.push(newSubDomain);
    }
    return categories;
  }

  // Helper: Remove domain (delete detection will handle it)
  removeDomain(categories, domainUuid) {
    categories.domains = categories.domains.filter(d => d.uuid !== domainUuid);
    return categories;
  }
}

// Usage Example
const categoryManager = new CategoryManager();

// Load existing categories
const categories = await categoryManager.getCategories('user-123');

// Add new domain
categoryManager.addNewDomain(categories, 'Work', 'Work-related activities');

// Update categories (backend will detect the new domain and create it)
const updatedCategories = await categoryManager.updateCategories(categories);
```

### Complete Workflow Example

```javascript
async function handleCategoryChanges() {
  const userId = 'user-123';
  const categoryManager = new CategoryManager();

  try {
    // 1. Get current categories
    let categories = await categoryManager.getCategories(userId);
    
    // 2. Make changes in UI (examples)
    
    // Add new domain
    categoryManager.addNewDomain(categories, 'Personal Development', 'Self-improvement activities');
    
    // Add subdomain to existing domain
    const healthDomain = categories.domains.find(d => d.name === 'Health');
    if (healthDomain) {
      categoryManager.addNewSubDomain(categories, healthDomain.uuid, 'Mental Health', 'Mental wellness activities');
    }
    
    // Update existing domain name
    if (healthDomain) {
      healthDomain.name = 'Health & Wellness';
    }
    
    // Remove a domain (by filtering it out)
    categories.domains = categories.domains.filter(d => d.name !== 'Unwanted Domain');
    
    // 3. Send all changes in one request (choose your preferred method)
    
    // Option 1: PUT method
    const result = await categoryManager.updateCategories(categories);
    
    // Option 2: PATCH method (for partial updates)
    // const result = await categoryManager.patchCategories(categories);
    
    // Option 3: POST method (alternative)
    // const result = await categoryManager.createOrUpdateCategories(categories);
    
    console.log('Categories updated successfully:', result);
    
  } catch (error) {
    console.error('Failed to update categories:', error);
  }
}
```

---

## 🧪 Testing Endpoints

For testing and development, use these endpoints:

### Get Sample Data
```http
GET /api/v1/test/categories/sample
```
Returns sample category structure for testing.

### Get API Documentation
```http
GET /api/v1/test/categories/flow-explanation
```
Returns detailed explanation of API flow.

---

## ⚡ Best Practices

### 1. Always Fetch Before Update
```javascript
// ✅ Correct approach
const categories = await getCategories(userId);
// Make changes to categories object
await updateCategories(categories);

// ❌ Avoid creating structure from scratch
// This might delete existing data
```

### 2. Preserve UUIDs
```javascript
// ✅ Keep existing UUIDs for updates
{
  "name": "Updated Name",
  "uuid": "existing-uuid-123", // Keep this!
  "description": "New description"
}

// ✅ Use empty UUID for new items
{
  "name": "New Item",
  "uuid": "", // Empty for creation
  "description": "This is new"
}
```

### 3. Handle Async Operations
```javascript
// ✅ Use proper error handling
try {
  const result = await updateCategories(data);
  // Handle success
} catch (error) {
  // Handle error
  console.error('Update failed:', error);
}
```

### 4. Batch Changes
```javascript
// ✅ Make multiple changes then send once
const categories = await getCategories(userId);

// Make all your changes
addNewDomain(categories, 'Domain 1');
addNewDomain(categories, 'Domain 2');
updateExistingDomain(categories, 'uuid-123', 'New Name');

// Send all changes together
await updateCategories(categories);
```

---

## 🚨 Error Handling

The API returns standard HTTP status codes:

- `200 OK`: Successful operation
- `400 Bad Request`: Invalid request data
- `404 Not Found`: User/Category not found
- `500 Internal Server Error`: Server error

**Error Response Format:**
```json
{
  "status": "error",
  "message": "Detailed error message",
  "data": null
}
```

---

## 🔍 Debugging Tips

1. **Check UUIDs**: Ensure existing items have valid UUIDs
2. **Validate Structure**: Make sure nested structure is properly formed
3. **Monitor Network**: Check request/response in browser dev tools
4. **Use Test Endpoints**: Leverage `/test/categories/sample` for testing

---

## 📞 Support

For questions or issues:
1. Check this documentation first
2. Test with sample endpoints
3. Verify request format matches examples
4. Contact backend team with specific error messages

---

## 🎯 Quick Reference

| Operation | UUID Value | Result |
|-----------|------------|---------|
| Create | `""` or missing | New item created |
| Update | Existing UUID | Item updated |
| Delete | Omit from request | Item deleted |

**Remember:** The frontend just sends the final desired state. The backend handles all the complex change detection automatically! 🚀
