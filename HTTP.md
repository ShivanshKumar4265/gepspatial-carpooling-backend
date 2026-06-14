| Operation Type              | HTTP Method | Use @Transactional?          | Recommended Settings                          | Example Method                     | Why? |
|-----------------------------|-------------|------------------------------|-----------------------------------------------|------------------------------------|------|
| Simple Create               | POST        | Yes (Recommended)            | `@Transactional`                              | `createPreference()`               | Ensures atomicity for check-then-act |
| Create with Multiple Writes | POST        | **Must**                     | `@Transactional`                              | `createRideOfferWithPreferences()` | Multiple inserts must be atomic |
| Read Only (Single)          | GET         | Optional                     | `@Transactional(readOnly = true)`             | `getPreferenceById()`              | Performance + avoids lazy loading issues |
| Read with Joins / Complex   | GET         | Yes                          | `@Transactional(readOnly = true)`             | `getRideOfferWithDetails()`        | Consistent view of related data |
| Update                      | PUT/PATCH   | **Must**                     | `@Transactional`                              | `updatePreference()`               | Read + Validate + Write must be atomic |
| Delete                      | DELETE      | **Must**                     | `@Transactional`                              | `deletePreference()`               | Safe deletion with checks/cascades |
| Batch / Long Running        | POST/PUT    | Yes                          | `@Transactional(timeout = 60)`                | `processBulkUpload()`              | Prevent transaction timeout |
| Independent Operations      | Any         | No                           | No annotation or `Propagation.REQUIRES_NEW`   | `logActivity()`                    | Don't rollback previous success |


- PUT modify the entire object
- PATCH partial update