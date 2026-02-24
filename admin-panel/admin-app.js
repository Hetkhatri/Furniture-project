// ==================== FIREBASE CONFIG ====================
const firebaseConfig = {
    apiKey: "AIzaSyDeYmJ3dfQNGkvPZ1CgAzSdES1q8uCMHMc",
    authDomain: "furniture-ecommerce-app-32829.firebaseapp.com",
    databaseURL: "https://furniture-ecommerce-app-32829-default-rtdb.firebaseio.com",
    projectId: "furniture-ecommerce-app-32829",
    storageBucket: "furniture-ecommerce-app-32829.firebasestorage.app",
    messagingSenderId: "262113820251",
    appId: "1:262113820251:android:0872236a5c9c352933097e"
};

firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const db = firebase.database();

// ==================== DOM REFERENCES ====================
const loginScreen = document.getElementById('login-screen');
const adminPanel = document.getElementById('admin-panel');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');

// ==================== AUTH STATE ====================
auth.onAuthStateChanged(user => {
    if (user) {
        checkAdminRole(user);
    } else {
        showLogin();
    }
});

function checkAdminRole(user) {
    db.ref('users/' + user.uid).once('value').then(snapshot => {
        const userData = snapshot.val();
        if (userData && userData.role === 'admin') {
            showAdminPanel(user, userData);
        } else {
            // If no user record exists or role isn't admin, 
            // still allow if this is the first admin setup
            db.ref('users').once('value').then(allUsers => {
                if (!allUsers.exists() || allUsers.numChildren() === 0) {
                    // No users exist yet — make this user admin
                    db.ref('users/' + user.uid).set({
                        uid: user.uid,
                        name: user.displayName || 'Admin',
                        email: user.email,
                        role: 'admin',
                        createdAt: Date.now()
                    }).then(() => {
                        showAdminPanel(user, { name: 'Admin', role: 'admin' });
                    });
                } else {
                    loginError.textContent = 'Access denied. You do not have admin privileges.';
                    auth.signOut();
                    showLogin();
                }
            });
        }
    });
}

// ==================== LOGIN / LOGOUT ====================
loginForm.addEventListener('submit', e => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    loginError.textContent = '';

    auth.signInWithEmailAndPassword(email, password)
        .catch(err => {
            loginError.textContent = err.message;
        });
});

document.getElementById('logout-btn').addEventListener('click', () => {
    auth.signOut();
});

function showLogin() {
    loginScreen.style.display = 'flex';
    adminPanel.style.display = 'none';
}

function showAdminPanel(user, userData) {
    loginScreen.style.display = 'none';
    adminPanel.style.display = 'flex';
    document.getElementById('admin-welcome').textContent =
        'Welcome, ' + (userData.name || user.email);
    loadDashboard();
    loadProducts();
    loadOrders();
    loadUsers();
}

// ==================== NAVIGATION ====================
document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', () => {
        document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
        document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
        item.classList.add('active');
        const section = item.getAttribute('data-section');
        document.getElementById('section-' + section).classList.add('active');
    });
});

// ==================== DASHBOARD ====================
function loadDashboard() {
    // Products count
    db.ref('products').on('value', snap => {
        document.getElementById('stat-products').textContent = snap.numChildren();
    });

    // Orders count + revenue + recent orders + breakdown
    db.ref('orders').on('value', snap => {
        const orders = [];
        let revenue = 0;
        const statusCounts = { pending: 0, confirmed: 0, shipped: 0, delivered: 0, cancelled: 0 };

        snap.forEach(child => {
            const order = child.val();
            order.id = child.key;
            orders.push(order);
            revenue += order.totalPrice || 0;
            const status = (order.status || 'pending').toLowerCase();
            if (statusCounts.hasOwnProperty(status)) statusCounts[status]++;
        });

        document.getElementById('stat-orders').textContent = orders.length;
        document.getElementById('stat-revenue').textContent = '₹' + revenue.toFixed(2);

        // Recent orders (last 5)
        orders.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
        const recentContainer = document.getElementById('recent-orders-list');
        if (orders.length === 0) {
            recentContainer.innerHTML = '<p class="empty-text">No orders yet</p>';
        } else {
            recentContainer.innerHTML = orders.slice(0, 5).map(o => `
                <div class="recent-order-item">
                    <div class="recent-order-info">
                        <strong>#${(o.id || '').substring(0, 8).toUpperCase()}</strong>
                        <span>${o.userName || o.shippingName || 'Unknown'} — ₹${(o.totalPrice || 0).toFixed(2)}</span>
                    </div>
                    <span class="badge badge-${(o.status || 'pending').toLowerCase()}">${capitalize(o.status || 'pending')}</span>
                </div>
            `).join('');
        }

        // Status breakdown
        const breakdownContainer = document.getElementById('order-status-breakdown');
        const statusColors = { pending: '#FF9800', confirmed: '#2196F3', shipped: '#9C27B0', delivered: '#4CAF50', cancelled: '#f44336' };
        breakdownContainer.innerHTML = Object.entries(statusCounts).map(([status, count]) => `
            <div class="status-row">
                <div class="status-label">
                    <span class="status-dot" style="background:${statusColors[status]}"></span>
                    ${capitalize(status)}
                </div>
                <span class="status-count">${count}</span>
            </div>
        `).join('');
    });

    // Users count
    db.ref('users').on('value', snap => {
        document.getElementById('stat-users').textContent = snap.numChildren();
    });
}

// ==================== PRODUCTS ====================
let allProducts = [];

function loadProducts() {
    db.ref('products').on('value', snap => {
        allProducts = [];
        snap.forEach(child => {
            const product = child.val();
            product.id = child.key;
            allProducts.push(product);
        });
        renderProductsTable(allProducts);
    });
}

function renderProductsTable(products) {
    const tbody = document.getElementById('products-table-body');
    if (products.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-text">No products found</td></tr>';
        return;
    }
    tbody.innerHTML = products.map(p => `
        <tr>
            <td><strong>${escapeHtml(p.name || '')}</strong></td>
            <td>${escapeHtml(p.category || '')}</td>
            <td>₹${(p.price || 0).toFixed(2)}</td>
            <td>${p.stock || 0}</td>
            <td>⭐ ${(p.rating || 0).toFixed(1)}</td>
            <td class="actions">
                <button class="btn btn-sm btn-primary" onclick="editProduct('${p.id}')">Edit</button>
                <button class="btn btn-sm btn-danger" onclick="deleteProduct('${p.id}', '${escapeHtml(p.name || '')}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

// Product search
document.getElementById('product-search').addEventListener('input', e => {
    const query = e.target.value.toLowerCase();
    const filtered = allProducts.filter(p =>
        (p.name || '').toLowerCase().includes(query) ||
        (p.category || '').toLowerCase().includes(query)
    );
    renderProductsTable(filtered);
});

// Add Product button
document.getElementById('add-product-btn').addEventListener('click', () => {
    openProductModal();
});

function openProductModal(product) {
    document.getElementById('product-modal').style.display = 'flex';
    const form = document.getElementById('product-form');
    form.reset();

    if (product) {
        document.getElementById('product-modal-title').textContent = 'Edit Product';
        document.getElementById('product-name').value = product.name || '';
        document.getElementById('product-category').value = product.category || 'Chairs';
        document.getElementById('product-description').value = product.description || '';
        document.getElementById('product-price').value = product.price || '';
        document.getElementById('product-stock').value = product.stock || '';
        document.getElementById('product-rating').value = product.rating || 4.0;
        document.getElementById('product-image').value = product.imageUrl || '';
        document.getElementById('product-edit-id').value = product.id;
    } else {
        document.getElementById('product-modal-title').textContent = 'Add Product';
        document.getElementById('product-edit-id').value = '';
    }
}

function closeProductModal() {
    document.getElementById('product-modal').style.display = 'none';
}

function editProduct(productId) {
    const product = allProducts.find(p => p.id === productId);
    if (product) openProductModal(product);
}

function deleteProduct(productId, productName) {
    if (confirm('Delete "' + productName + '"? This cannot be undone.')) {
        db.ref('products/' + productId).remove()
            .then(() => showToast('Product deleted'))
            .catch(err => showToast('Error: ' + err.message));
    }
}

// Product form submit
document.getElementById('product-form').addEventListener('submit', e => {
    e.preventDefault();
    const editId = document.getElementById('product-edit-id').value;
    const productData = {
        name: document.getElementById('product-name').value,
        category: document.getElementById('product-category').value,
        description: document.getElementById('product-description').value,
        price: parseFloat(document.getElementById('product-price').value),
        stock: parseInt(document.getElementById('product-stock').value),
        rating: parseFloat(document.getElementById('product-rating').value) || 4.0,
        imageUrl: document.getElementById('product-image').value || '',
        createdAt: Date.now()
    };

    if (editId) {
        // Update existing
        productData.id = editId;
        db.ref('products/' + editId).update(productData)
            .then(() => {
                closeProductModal();
                showToast('Product updated!');
            })
            .catch(err => showToast('Error: ' + err.message));
    } else {
        // Add new
        const newRef = db.ref('products').push();
        productData.id = newRef.key;
        newRef.set(productData)
            .then(() => {
                closeProductModal();
                showToast('Product added!');
            })
            .catch(err => showToast('Error: ' + err.message));
    }
});

// ==================== ORDERS ====================
let allOrders = [];

function loadOrders() {
    db.ref('orders').on('value', snap => {
        allOrders = [];
        snap.forEach(child => {
            const order = child.val();
            order.id = child.key;
            allOrders.push(order);
        });
        allOrders.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
        renderOrdersTable(allOrders);
    });
}

function renderOrdersTable(orders) {
    const tbody = document.getElementById('orders-table-body');
    if (orders.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-text">No orders found</td></tr>';
        return;
    }
    tbody.innerHTML = orders.map(o => {
        const date = new Date(o.createdAt || 0);
        const dateStr = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
        const itemCount = (o.items || []).length;
        const status = (o.status || 'pending').toLowerCase();
        return `
            <tr>
                <td><strong>#${(o.id || '').substring(0, 8).toUpperCase()}</strong></td>
                <td>${dateStr}</td>
                <td>${escapeHtml(o.userName || o.shippingName || 'Unknown')}</td>
                <td>${itemCount} item${itemCount !== 1 ? 's' : ''}</td>
                <td>₹${(o.totalPrice || 0).toFixed(2)}</td>
                <td><span class="badge badge-${status}">${capitalize(status)}</span></td>
                <td class="actions">
                    <button class="btn btn-sm btn-primary" onclick="viewOrder('${o.id}')">View</button>
                </td>
            </tr>
        `;
    }).join('');
}

// Order filter
document.getElementById('order-status-filter').addEventListener('change', e => {
    const filterVal = e.target.value;
    if (filterVal === 'all') {
        renderOrdersTable(allOrders);
    } else {
        const filtered = allOrders.filter(o => (o.status || 'pending').toLowerCase() === filterVal);
        renderOrdersTable(filtered);
    }
});

function viewOrder(orderId) {
    const order = allOrders.find(o => o.id === orderId);
    if (!order) return;

    const date = new Date(order.createdAt || 0);
    const dateStr = date.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    const status = (order.status || 'pending').toLowerCase();

    const itemsHtml = (order.items || []).map(item => `
        <div class="order-item-row">
            <span>${escapeHtml(item.productName || 'Unknown')} × ${item.quantity || 1}</span>
            <span>₹${((item.productPrice || 0) * (item.quantity || 1)).toFixed(2)}</span>
        </div>
    `).join('');

    document.getElementById('order-detail-content').innerHTML = `
        <div class="order-detail-grid">
            <div class="order-detail-section">
                <h4>Order Info</h4>
                <p><strong>ID:</strong> ${order.id}<br>
                <strong>Date:</strong> ${dateStr}<br>
                <strong>Status:</strong> <span class="badge badge-${status}">${capitalize(status)}</span></p>
            </div>
            <div class="order-detail-section">
                <h4>Customer</h4>
                <p><strong>Name:</strong> ${escapeHtml(order.userName || order.shippingName || 'N/A')}<br>
                <strong>Email:</strong> ${escapeHtml(order.userEmail || 'N/A')}<br>
                <strong>Phone:</strong> ${escapeHtml(order.phone || order.shippingPhone || 'N/A')}</p>
            </div>
            <div class="order-detail-section">
                <h4>Shipping Address</h4>
                <p>${escapeHtml(order.shippingAddress || 'N/A')}</p>
            </div>
            <div class="order-detail-section">
                <h4>Items</h4>
                ${itemsHtml || '<p>No items</p>'}
                <div class="order-item-row" style="border-top: 2px solid #eee; margin-top: 8px; padding-top: 8px;">
                    <span><strong>Total</strong></span>
                    <span><strong>₹${(order.totalPrice || 0).toFixed(2)}</strong></span>
                </div>
            </div>
            <div class="order-detail-section">
                <h4>Update Status</h4>
                <select class="order-status-select" onchange="updateOrderStatus('${order.id}', this.value)">
                    <option value="pending" ${status === 'pending' ? 'selected' : ''}>Pending</option>
                    <option value="confirmed" ${status === 'confirmed' ? 'selected' : ''}>Confirmed</option>
                    <option value="shipped" ${status === 'shipped' ? 'selected' : ''}>Shipped</option>
                    <option value="delivered" ${status === 'delivered' ? 'selected' : ''}>Delivered</option>
                    <option value="cancelled" ${status === 'cancelled' ? 'selected' : ''}>Cancelled</option>
                </select>
            </div>
        </div>
    `;

    document.getElementById('order-modal').style.display = 'flex';
}

function closeOrderModal() {
    document.getElementById('order-modal').style.display = 'none';
}

function updateOrderStatus(orderId, newStatus) {
    db.ref('orders/' + orderId + '/status').set(newStatus)
        .then(() => showToast('Order status updated to ' + capitalize(newStatus)))
        .catch(err => showToast('Error: ' + err.message));
}

// ==================== USERS ====================
function loadUsers() {
    db.ref('users').on('value', snap => {
        const tbody = document.getElementById('users-table-body');
        const users = [];
        snap.forEach(child => {
            const user = child.val();
            user.id = child.key;
            users.push(user);
        });

        if (users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="empty-text">No users found</td></tr>';
            return;
        }

        tbody.innerHTML = users.map(u => {
            const date = new Date(u.createdAt || 0);
            const dateStr = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
            const roleBadge = u.role === 'admin'
                ? '<span class="badge badge-confirmed">Admin</span>'
                : '<span class="badge badge-pending">User</span>';
            return `
                <tr>
                    <td><strong>${escapeHtml(u.name || 'Unknown')}</strong></td>
                    <td>${escapeHtml(u.email || '')}</td>
                    <td>${roleBadge}</td>
                    <td>${dateStr}</td>
                </tr>
            `;
        }).join('');
    });
}

// ==================== UTILITIES ====================
function capitalize(str) {
    if (!str) return '';
    return str.charAt(0).toUpperCase() + str.slice(1);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message) {
    // Simple toast notification
    let toast = document.getElementById('admin-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'admin-toast';
        toast.style.cssText = `
            position: fixed; bottom: 24px; right: 24px; 
            background: #333; color: white; padding: 12px 24px; 
            border-radius: 8px; font-size: 14px; z-index: 9999; 
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            transition: opacity 0.3s ease, transform 0.3s ease;
            opacity: 0; transform: translateY(10px);
        `;
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.style.opacity = '1';
    toast.style.transform = 'translateY(0)';

    clearTimeout(toast._timeout);
    toast._timeout = setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
    }, 3000);
}

// Close modals on backdrop click
document.querySelectorAll('.modal-backdrop').forEach(backdrop => {
    backdrop.addEventListener('click', () => {
        closeProductModal();
        closeOrderModal();
    });
});
