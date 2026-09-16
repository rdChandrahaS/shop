import { useCallback, useEffect, useMemo, useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import {
  Search, ShoppingBag, User, Heart, Menu, X, ChevronRight, ArrowRight,
  Star, Plus, Minus, Trash2, ShieldCheck, Truck, Sparkles,
  Package, Pencil, Trash, RefreshCw
} from "lucide-react";
import "./App.css";

const API = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/$/, "");
const CART_KEY = "shop-cart";
const WISHLIST_KEY = "shop-wishlist";
const TOKEN_KEY = "shop-token";
const ACCOUNT_KEY = "shop-account";

const categoryEmoji = (name) => {
  const value = String(name || "").toLowerCase();
  if (value.includes("burger")) return "🍔";
  if (value.includes("pizza")) return "🍕";
  if (value.includes("pasta")) return "🍝";
  if (value.includes("bowl")) return "🥗";
  if (value.includes("dessert") || value.includes("cake")) return "🍰";
  if (value.includes("drink") || value.includes("coffee")) return "🥤";
  if (value.includes("snack") || value.includes("fries")) return "🍟";
  if (value.includes("wrap")) return "🌯";
  if (value.includes("salad")) return "🥗";
  if (value.includes("breakfast")) return "🥞";
  return "🍽️";
};

function readJson(key, fallback) {
  try { return JSON.parse(localStorage.getItem(key) || JSON.stringify(fallback)); }
  catch { return fallback; }
}

function decodeJwt(token) {
  try {
    const payload = token.split(".")[1];
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(normalized));
  } catch { return null; }
}

function getRoles(token) {
  const claims = decodeJwt(token);
  const roles = claims?.realm_access?.roles;
  return Array.isArray(roles) ? roles.map(String).map(r => r.toUpperCase()) : [];
}

function money(v) { return `₹${Number(v || 0).toFixed(2)}`; }

function authHeaders() {
  const token = localStorage.getItem(TOKEN_KEY);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function apiRequest(path, options = {}) {
  const headers = { ...(options.body ? { "Content-Type": "application/json" } : {}), ...authHeaders(), ...(options.headers || {}) };
  const response = await fetch(`${API}${path}`, { ...options, headers });
  if (response.status === 204) return null;
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!response.ok) {
    const message = typeof data === "string" ? data : data?.message || data?.error || `Request failed (${response.status})`;
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return data;
}

function ProductVisual({ food, small = false }) {
  const [failed, setFailed] = useState(false);
  const emoji = categoryEmoji(food?.category);
  return (
    <div className={`product-visual ${small ? "small" : ""}`}>
      {food?.imageUrl && !failed ? (
        <img src={food.imageUrl} alt={food.foodName || "Food"} onError={() => setFailed(true)} />
      ) : <motion.span initial={{ scale: .7 }} whileInView={{ scale: 1 }} viewport={{ once: true }}>{emoji}</motion.span>}
    </div>
  );
}

function App() {
  const [products, setProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [productError, setProductError] = useState("");
  const [cart, setCart] = useState(() => readJson(CART_KEY, []));
  const [wishlist, setWishlist] = useState(() => readJson(WISHLIST_KEY, []));
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("All");
  const [cartOpen, setCartOpen] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState("login");
  const [account, setAccount] = useState(() => readJson(ACCOUNT_KEY, null));
  const [notice, setNotice] = useState("");
  const [ordersOpen, setOrdersOpen] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const token = localStorage.getItem(TOKEN_KEY);
  const claims = token ? decodeJwt(token) : null;
  const tokenExpired = claims?.exp ? claims.exp * 1000 <= Date.now() : false;
  const roles = token && !tokenExpired ? getRoles(token) : [];
  const isAdmin = roles.includes("ADMIN");
  const isUser = roles.includes("USER");

  const showNotice = useCallback((message) => {
    setNotice(message);
    window.setTimeout(() => setNotice(""), 2200);
  }, []);

  const loadProducts = useCallback(async () => {
    setProductError("");
    try {
      const data = await apiRequest("/foods");
      if (!Array.isArray(data)) throw new Error("Invalid menu response from server");
      setProducts(data.filter(food => food?.active !== false));
    } catch (error) {
      setProductError(error.message || "Unable to load the menu.");
    } finally { setLoadingProducts(false); setRefreshing(false); }
  }, []);

  useEffect(() => {
    if (tokenExpired) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(ACCOUNT_KEY);
      setAccount(null);
    }
  }, [tokenExpired]);
  useEffect(() => { loadProducts(); }, [loadProducts]);
  useEffect(() => localStorage.setItem(CART_KEY, JSON.stringify(cart)), [cart]);
  useEffect(() => localStorage.setItem(WISHLIST_KEY, JSON.stringify(wishlist)), [wishlist]);

  const categories = useMemo(() => [...new Set(products.map(p => p.category).filter(Boolean))].sort((a,b) => a.localeCompare(b)), [products]);
  const filtered = useMemo(() => products.filter(p => {
    const text = `${p.foodName || ""} ${p.foodDescription || ""} ${p.category || ""}`.toLowerCase();
    return (category === "All" || p.category === category) && text.includes(query.toLowerCase().trim());
  }), [products, query, category]);

  const count = cart.reduce((n, x) => n + x.qty, 0);
  const subtotal = cart.reduce((n, x) => n + Number(x.foodPrice) * x.qty, 0);
  const delivery = 0; // OrderingService currently charges only the server-calculated food total.

  const add = (food) => {
    setCart(current => {
      const found = current.find(x => x.foodId === food.foodId);
      return found
        ? current.map(x => x.foodId === food.foodId ? { ...x, qty: x.qty + 1, ...food } : x)
        : [...current, { ...food, qty: 1 }];
    });
    showNotice(`${food.foodName} added to cart`);
  };

  const changeQty = (id, delta) => setCart(current => current
    .map(x => x.foodId === id ? { ...x, qty: x.qty + delta } : x)
    .filter(x => x.qty > 0));

  const toggleWish = id => setWishlist(current => current.includes(id) ? current.filter(x => x !== id) : [...current, id]);

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ACCOUNT_KEY);
    setAccount(null);
    setOrdersOpen(false);
    setAdminOpen(false);
    showNotice("Signed out");
  };

  return (
    <div className="app">
      <div className="announcement">LIVE MENU <span>•</span> SECURE CHECKOUT THROUGH SHOP</div>
      <header className="header">
        <button className="icon-btn mobile-menu" aria-label="Menu"><Menu size={21}/></button>
        <a className="logo" href="#home"><span>shop</span><i>•</i></a>
        <nav>
          {["Home", "Shop", "Deals", "About"].map((x, i) => <a key={x} href={i === 0 ? "#home" : `#${x.toLowerCase()}`}>{x}</a>)}
        </nav>
        <div className="header-actions">
          <div className="search"><Search size={18}/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search food..." /></div>
          <button className="icon-btn" onClick={() => setAuthOpen(true)} title={account ? "Account" : "Sign in"}><User size={20}/>{account && <span className="status-dot"/>}</button>
          {account && <button className="icon-btn header-order-btn" onClick={() => setOrdersOpen(true)} title="Orders"><Package size={20}/></button>}
          <button className="icon-btn" onClick={() => setCartOpen(true)} title="Cart"><ShoppingBag size={21}/><b className="badge">{count}</b></button>
        </div>
      </header>

      <main id="home">
        <section className="hero">
          <div className="hero-copy">
            <motion.div className="eyebrow" initial={{opacity:0,y:20}} animate={{opacity:1,y:0}}>YOUR EVERYDAY CRAVINGS, UPGRADED <Sparkles size={14}/></motion.div>
            <motion.h1 initial={{opacity:0,y:30}} animate={{opacity:1,y:0}} transition={{delay:.08}}>Good food.<br/><em>Good mood.</em></motion.h1>
            <motion.p initial={{opacity:0,y:20}} animate={{opacity:1,y:0}} transition={{delay:.16}}>Freshly made favourites, delivered fast. Discover something delicious today.</motion.p>
            <div className="hero-actions"><a href="#shop" className="primary-btn">Shop now <ArrowRight size={18}/></a><a href="#deals" className="text-btn">View today's offer <ChevronRight size={16}/></a></div>
            <div className="trust-row"><span><Truck/> Fast delivery</span><span><ShieldCheck/> Secure checkout</span><span><Star/> Live menu</span></div>
          </div>
          <div className="hero-art">
            <motion.div className="orb orb-a" animate={{y:[0,-16,0],rotate:[0,5,0]}} transition={{duration:5,repeat:Infinity}}/>
            <motion.div className="orb orb-b" animate={{y:[0,14,0]}} transition={{duration:4,repeat:Infinity}}/>
            <HeroFood food={products[0]} />
            <div className="floating-pill p1">🔥 From today's menu</div><div className="floating-pill p2">Live catalogue</div>
          </div>
        </section>

        <section className="category-strip" id="shop">
          <div className="section-head"><div><span className="eyebrow">EXPLORE</span><h2>Shop by category</h2></div><a href="#products">See all <ArrowRight size={16}/></a></div>
          <div className="categories">
            <button className={category === "All" ? "active" : ""} onClick={() => setCategory("All")}><span>✨</span>All</button>
            {categories.map(c => <button key={c} className={category === c ? "active" : ""} onClick={() => setCategory(c)}><span>{categoryEmoji(c)}</span>{c}</button>)}
          </div>
        </section>

        <section className="deal-banner" id="deals">
          <div><span className="eyebrow">SHOPPING MADE SIMPLE</span><h2>Pick your favourites.<br/><em>We'll handle the rest.</em></h2><p>Your cart uses the live menu and prices supplied by the backend.</p><button className="light-btn" onClick={() => document.getElementById("products")?.scrollIntoView({behavior:"smooth"})}>Browse menu <ArrowRight size={17}/></button></div>
          <motion.div className="deal-emoji" animate={{rotate:[-4,4,-4],y:[0,-8,0]}} transition={{duration:3,repeat:Infinity}}>{categoryEmoji(products[0]?.category)}</motion.div>
        </section>

        <section className="products-section" id="products">
          <div className="section-head"><div><span className="eyebrow">FRESH PICKS</span><h2>Popular right now</h2></div><div className="result-count">{filtered.length} items</div></div>
          {loadingProducts ? <div className="loading-grid">{[1,2,3,4].map(i => <div className="skeleton-card" key={i}/>)}</div>
          : productError ? <div className="api-state"><h3>Couldn't load the menu</h3><p>{productError}</p><button className="primary-btn" onClick={() => { setLoadingProducts(true); setRefreshing(true); loadProducts(); }}>Retry</button></div>
          : filtered.length === 0 ? <div className="api-state"><h3>No products found</h3><p>Try another search or category.</p><button className="text-btn" onClick={() => {setQuery("");setCategory("All")}}>Clear filters</button></div>
          : <div className="product-grid">{filtered.map((p, i) => <motion.article className="product-card" key={p.foodId} layout initial={{opacity:0,y:20}} whileInView={{opacity:1,y:0}} viewport={{once:true,amount:.15}} transition={{delay:(i%4)*.05}}>
              <div className="product-image-wrap"><ProductVisual food={p}/><button className={`wish ${wishlist.includes(p.foodId) ? "liked" : ""}`} onClick={() => toggleWish(p.foodId)} aria-label="Wishlist"><Heart size={18} fill={wishlist.includes(p.foodId) ? "currentColor" : "none"}/></button>{p.active && <span className="product-tag">AVAILABLE</span>}</div>
              <div className="product-info"><div className="rating"><Star size={13} fill="currentColor"/> {p.category || "Menu item"}</div><h3>{p.foodName}</h3><p>{p.foodDescription}</p><div className="product-bottom"><strong>{money(p.foodPrice)}</strong><button className="add-btn" onClick={() => add(p)}><Plus size={17}/> Add</button></div></div>
            </motion.article>)}</div>}
        </section>

        <section className="perks">{[["🚀","Fast delivery","Built for quick checkout."],["🌿","Fresh menu","Products come from FoodService."],["💳","Secure checkout","Authenticated order flow."],["↩","Order tracking","View your orders anytime."]].map(([a,b,c]) => <div key={b}><span>{a}</span><div><strong>{b}</strong><small>{c}</small></div></div>)}</section>

        <section className="story" id="about"><div className="story-art"><span>{categoryEmoji(products[0]?.category)}</span><div className="mini-card">Live catalogue<br/><b>{products.length} items loaded.</b></div></div><div><span className="eyebrow">WHY SHOP</span><h2>Simple food, <em>done right.</em></h2><p>The storefront is connected directly to the Shop backend. Menu data, prices, categories and availability are read from FoodService instead of being hardcoded in the UI.</p><a className="text-btn" href="#shop">Explore the menu <ArrowRight size={17}/></a></div></section>

        <section className="newsletter"><span className="eyebrow">ACCOUNT</span><h2>Your orders, <em>all together.</em></h2><p>Sign in to place orders and view your order history from OrderingService.</p><button className="primary-btn" onClick={() => account ? setOrdersOpen(true) : setAuthOpen(true)}>{account ? "View my orders" : "Sign in"} <ArrowRight size={17}/></button></section>
      </main>

      <footer><div className="footer-main"><div><a className="logo" href="#home"><span>shop</span><i>•</i></a><p>Good food. Good mood.<br/>Made for your everyday.</p></div><div><b>Shop</b><a href="#products">All products</a><a href="#deals">Offers</a><a href="#shop">Categories</a></div><div><b>Help</b><a href="#about">About us</a><a href="#">Delivery info</a><a href="#">Contact</a></div><div><b>Account</b><a href="#" onClick={e=>{e.preventDefault();account ? setOrdersOpen(true) : setAuthOpen(true)}}>{account ? "Orders" : "Sign in"}</a><a href="#" onClick={e=>{e.preventDefault();setCartOpen(true)}}>Cart ({count})</a>{account && <a href="#" onClick={e=>{e.preventDefault();logout()}}>Sign out</a>}</div></div><div className="footer-bottom"><span>© 2026 shop. Built for good food.</span><span>Privacy · Terms · Cookies</span></div></footer>

      <AnimatePresence>{cartOpen && <CartDrawer cart={cart} subtotal={subtotal} delivery={delivery} changeQty={changeQty} onClose={()=>setCartOpen(false)} onCheckout={async customer => {
        if (!token || !account || !isUser) { setCartOpen(false); setAuthOpen(true); showNotice("A USER account is required to place an order."); return; }
        try {
          const order = { orderId:null, status:null, message:"Order placed from web storefront", totalAmount:null, customer, items:cart.map(x=>({foodId:x.foodId,name:x.foodName,quantity:x.qty,pricePerUnit:x.foodPrice})), mode:customer.paymentMode };
          delete order.customer.paymentMode;
          await apiRequest("/order", {method:"POST", body:JSON.stringify({...order, mode: customer.paymentMode})});
          setCart([]); setCartOpen(false); showNotice("Order submitted successfully 🎉");
        } catch (error) { if (error.status === 401 || error.status === 403) { logout(); setAuthOpen(true); } showNotice(error.message || "Order failed"); }
      }}/>}</AnimatePresence>
      <AnimatePresence>{authOpen && <AuthModal mode={authMode} setMode={setAuthMode} setAccount={setAccount} onClose={()=>setAuthOpen(false)} />}</AnimatePresence>
      <AnimatePresence>{ordersOpen && <OrdersModal onClose={()=>setOrdersOpen(false)} onError={showNotice} />}</AnimatePresence>
      <AnimatePresence>{adminOpen && isAdmin && <AdminModal products={products} onClose={()=>setAdminOpen(false)} onChanged={loadProducts} onNotice={showNotice} />}</AnimatePresence>
      {isAdmin && <button className="admin-fab" onClick={()=>setAdminOpen(true)} title="Admin catalogue"><Pencil size={17}/> Admin</button>}
      <AnimatePresence>{notice && <motion.div className="toast" initial={{opacity:0,y:20}} animate={{opacity:1,y:0}} exit={{opacity:0,y:20}}>{notice}</motion.div>}</AnimatePresence>
      {refreshing && <div className="refresh-indicator"><RefreshCw size={13}/> Refreshing menu</div>}
    </div>
  );
}

function HeroFood({food}) {
  const [failed, setFailed] = useState(false);
  return <motion.div className="hero-card" initial={{opacity:0,scale:.85,rotate:5}} animate={{opacity:1,scale:1,rotate:-3}} transition={{type:"spring",delay:.15}}>
    <div className="hero-food">{food?.imageUrl && !failed ? <img src={food.imageUrl} alt={food.foodName} onError={()=>setFailed(true)}/> : categoryEmoji(food?.category)}</div>
    <div><small>{food ? "FROM THE LIVE MENU" : "MENU LOADING"}</small><strong>{food?.foodName || "Discover today's menu"}</strong><span>{food?.foodDescription || "Fresh favourites are loaded directly from FoodService."}</span></div>
    <b>{food ? money(food.foodPrice) : "—"}</b>
  </motion.div>;
}

function CartDrawer({cart,subtotal,delivery,changeQty,onClose,onCheckout}) {
  const [checkout, setCheckout] = useState(false);
  const [customer, setCustomer] = useState({name:"",email:"",phoneNo:"",paymentMode:"COD"});
  const [submitting, setSubmitting] = useState(false);
  const submit = async e => { e.preventDefault(); setSubmitting(true); try { await onCheckout(customer); } finally { setSubmitting(false); } };
  return <motion.div className="overlay" initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} onClick={onClose}>
    <motion.aside className="drawer" initial={{x:"100%"}} animate={{x:0}} exit={{x:"100%"}} transition={{type:"spring",stiffness:280,damping:28}} onClick={e=>e.stopPropagation()}>
      <div className="drawer-head"><div><span className="eyebrow">YOUR BAG</span><h2>{checkout ? "Checkout" : "Shopping cart"}</h2></div><button className="icon-btn" onClick={onClose}><X/></button></div>
      {!checkout ? (cart.length===0 ? <div className="empty"><div>🛍️</div><h3>Your bag is empty</h3><p>Add something delicious and it'll appear here.</p></div> : <><div className="cart-list">{cart.map((x)=><div className="cart-item" key={x.foodId}><ProductVisual food={x} small/><div><h3>{x.foodName}</h3><strong>{money(x.foodPrice)}</strong><div className="qty"><button onClick={()=>changeQty(x.foodId,-1)}><Minus size={13}/></button><span>{x.qty}</span><button onClick={()=>changeQty(x.foodId,1)}><Plus size={13}/></button></div></div><button className="trash" onClick={()=>changeQty(x.foodId,-99)}><Trash2 size={16}/></button></div>)}</div><CartSummary subtotal={subtotal} delivery={delivery} onCheckout={()=>setCheckout(true)}/></>) : <form className="checkout-form" onSubmit={submit}>
        <p className="checkout-note">OrderingService calculates the final total and server-side prices from the live catalogue.</p>
        <input required minLength="2" maxLength="100" placeholder="Full name" value={customer.name} onChange={e=>setCustomer({...customer,name:e.target.value})}/>
        <input required type="email" maxLength="180" placeholder="Email" value={customer.email} onChange={e=>setCustomer({...customer,email:e.target.value})}/>
        <input required pattern="[0-9+() -]{7,20}" placeholder="Phone number" value={customer.phoneNo} onChange={e=>setCustomer({...customer,phoneNo:e.target.value})}/>
        <select value={customer.paymentMode} onChange={e=>setCustomer({...customer,paymentMode:e.target.value})}><option value="COD">Cash on Delivery</option><option value="UPI">UPI</option><option value="ONLINE">Online payment</option></select>
        <div className="checkout-total"><span>Estimated total</span><b>{money(subtotal+delivery)}</b></div>
        <button type="submit" className="primary-btn full" disabled={submitting}>{submitting ? "Submitting…" : "Place order"} <ArrowRight size={17}/></button>
        <button type="button" className="text-btn back-checkout" onClick={()=>setCheckout(false)}>Back to cart</button>
      </form>}
    </motion.aside>
  </motion.div>;
}

function CartSummary({subtotal,delivery,onCheckout}) { return <div className="cart-summary"><div><span>Subtotal</span><b>{money(subtotal)}</b></div><div><span>Delivery</span><b>{delivery===0?"FREE":money(delivery)}</b></div><div className="total"><span>Total</span><b>{money(subtotal+delivery)}</b></div><button className="primary-btn full" onClick={onCheckout}>Checkout <ArrowRight size={17}/></button><small><ShieldCheck size={13}/> Secure authenticated checkout</small></div>; }

function AuthModal({mode,setMode,setAccount,onClose}) {
  const [form,setForm]=useState({username:"",email:"",name:"",password:""});
  const [loading,setLoading]=useState(false); const [error,setError]=useState("");
  const submit=async e=>{e.preventDefault();setLoading(true);setError("");try{
    if(mode==="login") {
      const d=await apiRequest("/auth/login",{method:"POST",body:JSON.stringify({username:form.username.trim(),password:form.password})});
      localStorage.setItem(TOKEN_KEY,d.token); localStorage.setItem(ACCOUNT_KEY,JSON.stringify(d)); setAccount(d); onClose();
    } else {
      await apiRequest("/auth/register",{method:"POST",body:JSON.stringify({username:form.username.trim(),email:form.email.trim(),name:form.name.trim(),password:form.password})});
      setMode("login"); setForm(f=>({...f,email:"",name:"",password:""})); setError("Account created. Please sign in.");
    }
  }catch(err){setError(err.message||"Unable to connect to the server.");}finally{setLoading(false)}};
  return <motion.div className="overlay" initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} onClick={onClose}><motion.div className="auth-modal" initial={{opacity:0,y:30,scale:.97}} animate={{opacity:1,y:0,scale:1}} onClick={e=>e.stopPropagation()}><button className="close-auth" onClick={onClose}><X/></button><span className="eyebrow">WELCOME TO SHOP</span><h2>{mode==="login"?"Welcome back.":"Create your account."}</h2><p>{mode==="login"?"Sign in to keep your orders and favourites together.":"Registration creates a USER account."}</p><form onSubmit={submit}>{mode==="register"&&<><input required minLength="2" maxLength="120" placeholder="Full name" value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/><input required type="email" maxLength="254" placeholder="Email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></>}<input required minLength="3" maxLength="50" placeholder="Username" value={form.username} onChange={e=>setForm({...form,username:e.target.value})}/><input required minLength="8" maxLength="100" type="password" placeholder="Password" value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/>{error&&<div className="form-msg">{error}</div>}<button className="primary-btn full" disabled={loading}>{loading?"Please wait…":mode==="login"?"Sign in":"Create account"} <ArrowRight size={17}/></button></form><button className="switch-auth" onClick={()=>{setMode(mode==="login"?"register":"login");setError("")}}>{mode==="login"?"New here? Create an account":"Already have an account? Sign in"}</button></motion.div></motion.div>;
}

function OrdersModal({onClose,onError}) {
  const [page,setPage]=useState(0); const [data,setData]=useState(null); const [loading,setLoading]=useState(true);
  const load=useCallback(async()=>{setLoading(true);try{setData(await apiRequest(`/order?page=${page}&size=10`));}catch(e){onError(e.message);}finally{setLoading(false);}},[page,onError]);
  useEffect(()=>{load();},[load]);
  const orders=data?.content || [];
  return <motion.div className="overlay" initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} onClick={onClose}><motion.div className="orders-modal" onClick={e=>e.stopPropagation()}><div className="drawer-head"><div><span className="eyebrow">ACCOUNT</span><h2>My orders</h2></div><button className="icon-btn" onClick={onClose}><X/></button></div>{loading?<div className="api-state">Loading orders…</div>:orders.length===0?<div className="empty"><div>📦</div><h3>No orders yet</h3><p>Your completed checkout orders will appear here.</p></div>:<div className="orders-list">{orders.map(order=><OrderCard key={order.orderId} order={order}/>)}</div>}<div className="pagination"><button disabled={page===0} onClick={()=>setPage(p=>p-1)}>Previous</button><span>Page {page+1}</span><button disabled={!data || data.last} onClick={()=>setPage(p=>p+1)}>Next</button></div></motion.div></motion.div>;
}

function OrderCard({order}) { return <article className="order-card"><div><span className="order-id">#{order.orderId}</span><strong>{order.orderStatus}</strong></div><small>{order.orderDate ? new Date(order.orderDate).toLocaleString() : ""}</small><div className="order-items">{(order.orderDetails || []).map((item,i)=><span key={i}>{item.name} × {item.quantity}</span>)}</div><b>{money(order.totalAmount)}</b></article>; }

function AdminModal({products,onClose,onChanged,onNotice}) {
  const empty={foodName:"",foodDescription:"",foodPrice:"",imageUrl:"",category:"",active:true};
  const [form,setForm]=useState(empty); const [editing,setEditing]=useState(null); const [loading,setLoading]=useState(false);
  const submit=async e=>{e.preventDefault();setLoading(true);try{const body={...form,foodPrice:Number(form.foodPrice)};if(editing) await apiRequest(`/foods/${editing}`,{method:"PUT",body:JSON.stringify(body)});else await apiRequest("/foods",{method:"POST",body:JSON.stringify(body)});setForm(empty);setEditing(null);await onChanged();onNotice(editing?"Food updated":"Food added");}catch(e){onNotice(e.message);}finally{setLoading(false)}};
  const edit=p=>{setEditing(p.foodId);setForm({foodName:p.foodName,foodDescription:p.foodDescription,foodPrice:p.foodPrice,imageUrl:p.imageUrl,category:p.category,active:p.active});};
  const remove=async id=>{if(!window.confirm("Delete this food item?"))return;try{await apiRequest(`/foods/${id}`,{method:"DELETE"});await onChanged();onNotice("Food deleted");}catch(e){onNotice(e.message)}};
  return <motion.div className="overlay" initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} onClick={onClose}><motion.div className="admin-modal" onClick={e=>e.stopPropagation()}><div className="drawer-head"><div><span className="eyebrow">ADMIN</span><h2>Catalogue management</h2></div><button className="icon-btn" onClick={onClose}><X/></button></div><form className="admin-form" onSubmit={submit}><input required minLength="2" maxLength="120" placeholder="Food name" value={form.foodName} onChange={e=>setForm({...form,foodName:e.target.value})}/><textarea required maxLength="500" placeholder="Description" value={form.foodDescription} onChange={e=>setForm({...form,foodDescription:e.target.value})}/><input required min="0.01" step="0.01" type="number" placeholder="Price" value={form.foodPrice} onChange={e=>setForm({...form,foodPrice:e.target.value})}/><input required maxLength="500" type="url" placeholder="Image URL" value={form.imageUrl} onChange={e=>setForm({...form,imageUrl:e.target.value})}/><input required minLength="2" maxLength="60" placeholder="Category" value={form.category} onChange={e=>setForm({...form,category:e.target.value})}/><label className="check-row"><input type="checkbox" checked={form.active} onChange={e=>setForm({...form,active:e.target.checked})}/> Active</label><button className="primary-btn full" disabled={loading}>{loading?"Saving…":editing?"Update food":"Add food"}</button>{editing&&<button type="button" className="text-btn" onClick={()=>{setEditing(null);setForm(empty)}}>Cancel edit</button>}</form><div className="admin-list">{products.map(p=><div className="admin-row" key={p.foodId}><ProductVisual food={p} small/><div><b>{p.foodName}</b><small>{p.category} · {money(p.foodPrice)}</small></div><button onClick={()=>edit(p)}><Pencil size={15}/></button><button onClick={()=>remove(p.foodId)}><Trash size={15}/></button></div>)}</div></motion.div></motion.div>;
}

export default App;
