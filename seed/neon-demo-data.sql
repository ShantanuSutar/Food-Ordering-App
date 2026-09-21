-- DineHub production-demo seed data for PostgreSQL / Neon.
--
-- Prerequisites:
--   1. Deploy and start the backend once so Hibernate creates/updates the schema.
--   2. Run this complete file once in the Neon SQL Editor.
--   3. All demo accounts use password: Demo@123
--
-- This script intentionally aborts if it detects its marker account. It does
-- not delete or overwrite any existing application data.
-- Seeded Stripe identifiers are demo history only; create all future payments
-- through Stripe Checkout.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email = 'admin@dinehub.demo') THEN
        RAISE EXCEPTION 'DineHub demo data is already installed';
    END IF;
END
$$;

-- USER_ROLE ordinals: 0 = customer, 1 = restaurant owner, 2 = admin.
INSERT INTO users (id, full_name, email, password, role)
SELECT nextval('users_seq'), full_name, email,
       crypt('Demo@123', gen_salt('bf', 10)), role::smallint
FROM (VALUES
    ('DineHub Administrator', 'admin@dinehub.demo', 2),
    ('Arjun Mehta', 'owner.spice@dinehub.demo', 1),
    ('Meera Shah', 'owner.bombay@dinehub.demo', 1),
    ('Karthik Iyer', 'owner.southern@dinehub.demo', 1),
    ('Giulia Romano', 'owner.napoli@dinehub.demo', 1),
    ('Aisha Khan', 'owner.greenbowl@dinehub.demo', 1),
    ('Priya Sharma', 'priya@dinehub.demo', 0),
    ('Rohan Verma', 'rohan@dinehub.demo', 0),
    ('Ananya Rao', 'ananya@dinehub.demo', 0),
    ('Kabir Singh', 'kabir@dinehub.demo', 0)
) AS seed_users(full_name, email, role);

-- Every account created by the real signup flow receives a cart, so the seed
-- mirrors that invariant even for owner/admin accounts.
INSERT INTO cart (id, customer_id, total)
SELECT nextval('cart_seq'), u.id, 0
FROM users u
WHERE u.email LIKE '%@dinehub.demo';

-- Restaurants and their owned address records.
WITH new_address AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Spice Route', '18 Koregaon Park Road', 'Pune', 'Maharashtra', '411001', 'India')
    RETURNING id
)
INSERT INTO restaurant (
    id, owner_id, name, description, cuisine_type, address_id,
    email, mobile, twitter, instagram, opening_hours, registration_date, open
)
SELECT nextval('restaurant_seq'), u.id, 'Spice Route',
       'North Indian classics, smoky tandoor dishes and slow-cooked curries.',
       'North Indian', a.id, 'spiceroute@dinehub.demo', '+91 90000 10001',
       '@SpiceRouteDineHub', '@spiceroute.dinehub',
       'Mon-Sun: 11:00 AM - 11:00 PM', current_timestamp - interval '14 months', true
FROM users u CROSS JOIN new_address a
WHERE u.email = 'owner.spice@dinehub.demo';

WITH new_address AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Bombay Street Kitchen', '42 Linking Road, Bandra West', 'Mumbai', 'Maharashtra', '400050', 'India')
    RETURNING id
)
INSERT INTO restaurant (
    id, owner_id, name, description, cuisine_type, address_id,
    email, mobile, twitter, instagram, opening_hours, registration_date, open
)
SELECT nextval('restaurant_seq'), u.id, 'Bombay Street Kitchen',
       'Mumbai street-food favourites served fresh, fast and full of flavour.',
       'Indian Street Food', a.id, 'bombaystreet@dinehub.demo', '+91 90000 10002',
       '@BombayStreetDH', '@bombaystreetkitchen',
       'Mon-Sun: 8:00 AM - 12:00 AM', current_timestamp - interval '11 months', true
FROM users u CROSS JOIN new_address a
WHERE u.email = 'owner.bombay@dinehub.demo';

WITH new_address AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Southern Stories', '77 Indiranagar 12th Main', 'Bengaluru', 'Karnataka', '560038', 'India')
    RETURNING id
)
INSERT INTO restaurant (
    id, owner_id, name, description, cuisine_type, address_id,
    email, mobile, twitter, instagram, opening_hours, registration_date, open
)
SELECT nextval('restaurant_seq'), u.id, 'Southern Stories',
       'Comforting South Indian breakfasts, regional meals and filter coffee.',
       'South Indian', a.id, 'southernstories@dinehub.demo', '+91 90000 10003',
       '@SouthernStories', '@southernstories',
       'Mon-Sun: 7:00 AM - 10:30 PM', current_timestamp - interval '9 months', true
FROM users u CROSS JOIN new_address a
WHERE u.email = 'owner.southern@dinehub.demo';

WITH new_address AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Napoli Hearth', '9 Hauz Khas Village', 'New Delhi', 'Delhi', '110016', 'India')
    RETURNING id
)
INSERT INTO restaurant (
    id, owner_id, name, description, cuisine_type, address_id,
    email, mobile, twitter, instagram, opening_hours, registration_date, open
)
SELECT nextval('restaurant_seq'), u.id, 'Napoli Hearth',
       'Hand-stretched pizzas, comforting pasta and classic Italian desserts.',
       'Italian', a.id, 'napoli@dinehub.demo', '+91 90000 10004',
       '@NapoliHearth', '@napolihearth',
       'Tue-Sun: 12:00 PM - 11:30 PM', current_timestamp - interval '7 months', true
FROM users u CROSS JOIN new_address a
WHERE u.email = 'owner.napoli@dinehub.demo';

WITH new_address AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Green Bowl Kitchen', '25 Jubilee Hills Road 36', 'Hyderabad', 'Telangana', '500033', 'India')
    RETURNING id
)
INSERT INTO restaurant (
    id, owner_id, name, description, cuisine_type, address_id,
    email, mobile, twitter, instagram, opening_hours, registration_date, open
)
SELECT nextval('restaurant_seq'), u.id, 'Green Bowl Kitchen',
       'Wholesome bowls, salads, smoothies and protein-forward everyday meals.',
       'Healthy & Continental', a.id, 'greenbowl@dinehub.demo', '+91 90000 10005',
       '@GreenBowlDH', '@greenbowl.kitchen',
       'Mon-Sat: 9:00 AM - 10:00 PM', current_timestamp - interval '5 months', false
FROM users u CROSS JOIN new_address a
WHERE u.email = 'owner.greenbowl@dinehub.demo';

INSERT INTO restaurant_images (restaurant_id, images)
SELECT r.id, v.image
FROM (VALUES
    ('Spice Route', 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c5?auto=format&fit=crop&w=1400&q=80'),
    ('Spice Route', 'https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=1400&q=80'),
    ('Bombay Street Kitchen', 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=1400&q=80'),
    ('Bombay Street Kitchen', 'https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=1400&q=80'),
    ('Southern Stories', 'https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=1400&q=80'),
    ('Southern Stories', 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=1400&q=80'),
    ('Napoli Hearth', 'https://images.unsplash.com/photo-1579751626657-72bc17010498?auto=format&fit=crop&w=1400&q=80'),
    ('Napoli Hearth', 'https://images.unsplash.com/photo-1579684947550-22e945225d9a?auto=format&fit=crop&w=1400&q=80'),
    ('Green Bowl Kitchen', 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?auto=format&fit=crop&w=1400&q=80'),
    ('Green Bowl Kitchen', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1400&q=80')
) AS v(restaurant_name, image)
JOIN restaurant r ON r.name = v.restaurant_name;

-- Menu categories.
INSERT INTO category (id, restaurant_id, name)
SELECT nextval('category_seq'), r.id, v.category_name
FROM (VALUES
    ('Spice Route', 'Starters'),
    ('Spice Route', 'Main Course'),
    ('Spice Route', 'Breads & Rice'),
    ('Spice Route', 'Beverages & Desserts'),
    ('Bombay Street Kitchen', 'Street Snacks'),
    ('Bombay Street Kitchen', 'Chaat'),
    ('Bombay Street Kitchen', 'Main Plates'),
    ('Bombay Street Kitchen', 'Drinks & Desserts'),
    ('Southern Stories', 'Breakfast'),
    ('Southern Stories', 'Dosa & Uttapam'),
    ('Southern Stories', 'Regional Mains'),
    ('Southern Stories', 'Drinks & Sweets'),
    ('Napoli Hearth', 'Pizza'),
    ('Napoli Hearth', 'Pasta'),
    ('Napoli Hearth', 'Sides'),
    ('Napoli Hearth', 'Desserts & Drinks'),
    ('Green Bowl Kitchen', 'Power Bowls'),
    ('Green Bowl Kitchen', 'Salads & Toasts'),
    ('Green Bowl Kitchen', 'Healthy Plates'),
    ('Green Bowl Kitchen', 'Cold Drinks')
) AS v(restaurant_name, category_name)
JOIN restaurant r ON r.name = v.restaurant_name;

-- Ingredient groups and selectable ingredients.
INSERT INTO ingredient_category (id, restaurant_id, name)
SELECT nextval('ingredient_category_seq'), r.id, v.category_name
FROM (VALUES
    ('Spice Route', 'Protein'), ('Spice Route', 'Add-ons'), ('Spice Route', 'Spice Level'),
    ('Bombay Street Kitchen', 'Bread'), ('Bombay Street Kitchen', 'Toppings'), ('Bombay Street Kitchen', 'Spice Level'),
    ('Southern Stories', 'Batter & Base'), ('Southern Stories', 'Add-ons'), ('Southern Stories', 'Spice Level'),
    ('Napoli Hearth', 'Cheese'), ('Napoli Hearth', 'Toppings'), ('Napoli Hearth', 'Extras'),
    ('Green Bowl Kitchen', 'Protein'), ('Green Bowl Kitchen', 'Add-ons'), ('Green Bowl Kitchen', 'Dressings')
) AS v(restaurant_name, category_name)
JOIN restaurant r ON r.name = v.restaurant_name;

INSERT INTO ingredients_item (id, category_id, restaurant_id, name, in_stock)
SELECT nextval('ingredients_item_seq'), ic.id, r.id, v.item_name, v.in_stock
FROM (VALUES
    ('Spice Route', 'Protein', 'Paneer', true),
    ('Spice Route', 'Protein', 'Chicken', true),
    ('Spice Route', 'Add-ons', 'Extra Butter', true),
    ('Spice Route', 'Add-ons', 'Extra Cheese', true),
    ('Spice Route', 'Spice Level', 'Mild', true),
    ('Spice Route', 'Spice Level', 'Medium', true),
    ('Spice Route', 'Spice Level', 'Hot', true),
    ('Bombay Street Kitchen', 'Bread', 'Pav', true),
    ('Bombay Street Kitchen', 'Bread', 'Brown Bread', true),
    ('Bombay Street Kitchen', 'Toppings', 'Onion', true),
    ('Bombay Street Kitchen', 'Toppings', 'Cheese', true),
    ('Bombay Street Kitchen', 'Toppings', 'Sev', true),
    ('Bombay Street Kitchen', 'Spice Level', 'Mild', true),
    ('Bombay Street Kitchen', 'Spice Level', 'Medium', true),
    ('Bombay Street Kitchen', 'Spice Level', 'Hot', true),
    ('Southern Stories', 'Batter & Base', 'Rice Batter', true),
    ('Southern Stories', 'Batter & Base', 'Ragi Batter', true),
    ('Southern Stories', 'Add-ons', 'Ghee', true),
    ('Southern Stories', 'Add-ons', 'Podi', true),
    ('Southern Stories', 'Add-ons', 'Coconut Chutney', true),
    ('Southern Stories', 'Spice Level', 'Mild', true),
    ('Southern Stories', 'Spice Level', 'Medium', true),
    ('Southern Stories', 'Spice Level', 'Hot', true),
    ('Napoli Hearth', 'Cheese', 'Mozzarella', true),
    ('Napoli Hearth', 'Cheese', 'Parmesan', true),
    ('Napoli Hearth', 'Cheese', 'Vegan Cheese', false),
    ('Napoli Hearth', 'Toppings', 'Olives', true),
    ('Napoli Hearth', 'Toppings', 'Mushrooms', true),
    ('Napoli Hearth', 'Toppings', 'Jalapenos', true),
    ('Napoli Hearth', 'Toppings', 'Pepperoni', true),
    ('Napoli Hearth', 'Extras', 'Garlic Dip', true),
    ('Green Bowl Kitchen', 'Protein', 'Paneer', true),
    ('Green Bowl Kitchen', 'Protein', 'Grilled Chicken', true),
    ('Green Bowl Kitchen', 'Protein', 'Tofu', true),
    ('Green Bowl Kitchen', 'Add-ons', 'Avocado', true),
    ('Green Bowl Kitchen', 'Add-ons', 'Hummus', true),
    ('Green Bowl Kitchen', 'Add-ons', 'Roasted Seeds', true),
    ('Green Bowl Kitchen', 'Dressings', 'Mint Yogurt', true),
    ('Green Bowl Kitchen', 'Dressings', 'Lemon Tahini', true),
    ('Green Bowl Kitchen', 'Dressings', 'Balsamic', true)
) AS v(restaurant_name, category_name, item_name, in_stock)
JOIN restaurant r ON r.name = v.restaurant_name
JOIN ingredient_category ic ON ic.restaurant_id = r.id AND ic.name = v.category_name;

-- Forty available menu items. Prices are stored in rupees.
INSERT INTO food (
    id, restaurant_id, food_category_id, name, description, price,
    available, is_vegetarian, is_seasonal, creation_date
)
SELECT nextval('food_seq'), r.id, c.id, v.food_name, v.description, v.price,
       v.available, v.vegetarian, v.seasonal,
       current_timestamp - make_interval(days => v.age_days)
FROM (VALUES
    ('Spice Route', 'Starters', 'Paneer Tikka', 'Char-grilled cottage cheese with peppers, onions and aromatic spices.', 249::bigint, true, true, false, 140),
    ('Spice Route', 'Starters', 'Tandoori Chicken', 'Yogurt-marinated chicken roasted in the tandoor.', 329::bigint, true, false, false, 132),
    ('Spice Route', 'Main Course', 'Butter Chicken', 'Tender chicken in a creamy tomato and butter gravy.', 349::bigint, true, false, false, 125),
    ('Spice Route', 'Main Course', 'Dal Makhani', 'Slow-cooked black lentils finished with butter and cream.', 229::bigint, true, true, false, 118),
    ('Spice Route', 'Breads & Rice', 'Vegetable Biryani', 'Fragrant basmati rice layered with vegetables and saffron.', 259::bigint, true, true, false, 110),
    ('Spice Route', 'Breads & Rice', 'Garlic Naan', 'Tandoor-baked flatbread topped with garlic and coriander.', 69::bigint, true, true, false, 102),
    ('Spice Route', 'Beverages & Desserts', 'Gulab Jamun', 'Warm milk dumplings soaked in cardamom syrup.', 99::bigint, true, true, false, 95),
    ('Spice Route', 'Beverages & Desserts', 'Mango Lassi', 'Chilled yogurt drink blended with ripe mango.', 99::bigint, true, true, true, 88),

    ('Bombay Street Kitchen', 'Street Snacks', 'Vada Pav', 'Crisp potato fritter, chutneys and chilli inside a soft pav.', 59::bigint, true, true, false, 130),
    ('Bombay Street Kitchen', 'Main Plates', 'Pav Bhaji', 'Buttery spiced vegetable mash with toasted pav.', 179::bigint, true, true, false, 124),
    ('Bombay Street Kitchen', 'Main Plates', 'Misal Pav', 'Sprouted moth-bean curry with farsan, onion and pav.', 149::bigint, true, true, false, 117),
    ('Bombay Street Kitchen', 'Chaat', 'Dahi Puri', 'Crisp puris filled with potato, yogurt and tangy chutneys.', 129::bigint, true, true, false, 109),
    ('Bombay Street Kitchen', 'Street Snacks', 'Bombay Sandwich', 'Triple-layer vegetable sandwich with green chutney.', 149::bigint, true, true, false, 101),
    ('Bombay Street Kitchen', 'Main Plates', 'Keema Pav', 'Spiced minced chicken served with butter-toasted pav.', 229::bigint, true, false, false, 93),
    ('Bombay Street Kitchen', 'Drinks & Desserts', 'Masala Chaas', 'Cooling buttermilk with cumin, coriander and ginger.', 69::bigint, true, true, false, 85),
    ('Bombay Street Kitchen', 'Drinks & Desserts', 'Rose Falooda', 'Rose milk, basil seeds, vermicelli and ice cream.', 149::bigint, true, true, true, 77),

    ('Southern Stories', 'Breakfast', 'Idli Vada', 'Steamed idlis and crisp medu vada with sambar and chutney.', 109::bigint, true, true, false, 120),
    ('Southern Stories', 'Dosa & Uttapam', 'Masala Dosa', 'Crisp rice crepe filled with spiced potato masala.', 139::bigint, true, true, false, 114),
    ('Southern Stories', 'Dosa & Uttapam', 'Ghee Podi Dosa', 'Golden dosa finished with ghee and house podi.', 179::bigint, true, true, false, 108),
    ('Southern Stories', 'Dosa & Uttapam', 'Vegetable Uttapam', 'Thick savory pancake topped with fresh vegetables.', 159::bigint, true, true, false, 99),
    ('Southern Stories', 'Regional Mains', 'Lemon Rice', 'Tempered rice with lemon, peanuts and curry leaves.', 149::bigint, true, true, false, 91),
    ('Southern Stories', 'Regional Mains', 'Chettinad Chicken', 'Peppery Tamil-style chicken curry with steamed rice.', 299::bigint, true, false, false, 84),
    ('Southern Stories', 'Drinks & Sweets', 'Filter Coffee', 'Strong South Indian coffee with hot frothy milk.', 69::bigint, true, true, false, 76),
    ('Southern Stories', 'Drinks & Sweets', 'Payasam', 'Creamy vermicelli pudding with cardamom and cashews.', 119::bigint, true, true, true, 68),

    ('Napoli Hearth', 'Pizza', 'Margherita Pizza', 'San Marzano tomato, mozzarella, basil and olive oil.', 299::bigint, true, true, false, 112),
    ('Napoli Hearth', 'Pizza', 'Farmhouse Pizza', 'Bell peppers, mushrooms, onions, olives and mozzarella.', 399::bigint, true, true, false, 104),
    ('Napoli Hearth', 'Pizza', 'Pepperoni Pizza', 'Mozzarella, tomato and generous slices of pepperoni.', 449::bigint, true, false, false, 96),
    ('Napoli Hearth', 'Pasta', 'Pasta Alfredo', 'Fettuccine in a parmesan cream sauce with mushrooms.', 329::bigint, true, true, false, 89),
    ('Napoli Hearth', 'Pasta', 'Penne Arrabbiata', 'Penne tossed in a spicy tomato and garlic sauce.', 299::bigint, true, true, false, 81),
    ('Napoli Hearth', 'Sides', 'Garlic Bread', 'Toasted artisan bread with garlic butter and herbs.', 149::bigint, true, true, false, 73),
    ('Napoli Hearth', 'Desserts & Drinks', 'Tiramisu', 'Coffee-soaked sponge layered with mascarpone cream.', 199::bigint, true, true, false, 66),
    ('Napoli Hearth', 'Desserts & Drinks', 'Lemon Iced Tea', 'Fresh-brewed tea, lemon and a touch of cane sugar.', 99::bigint, true, true, false, 58),

    ('Green Bowl Kitchen', 'Power Bowls', 'Buddha Bowl', 'Quinoa, chickpeas, roasted vegetables, greens and tahini.', 279::bigint, true, true, false, 100),
    ('Green Bowl Kitchen', 'Salads & Toasts', 'Grilled Paneer Salad', 'Grilled paneer, greens, cucumber, tomato and mint yogurt.', 259::bigint, true, true, false, 92),
    ('Green Bowl Kitchen', 'Power Bowls', 'Chicken Protein Bowl', 'Grilled chicken, brown rice, beans, vegetables and salsa.', 329::bigint, true, false, false, 84),
    ('Green Bowl Kitchen', 'Salads & Toasts', 'Avocado Toast', 'Sourdough, smashed avocado, cherry tomato and seeds.', 249::bigint, true, true, false, 75),
    ('Green Bowl Kitchen', 'Healthy Plates', 'Hummus Platter', 'Classic hummus with vegetables, olives and pita bread.', 229::bigint, true, true, false, 67),
    ('Green Bowl Kitchen', 'Healthy Plates', 'Quinoa Khichdi', 'Quinoa and lentils cooked with vegetables and gentle spices.', 269::bigint, true, true, false, 59),
    ('Green Bowl Kitchen', 'Cold Drinks', 'Berry Smoothie', 'Mixed berries, banana, yogurt and chia seeds.', 169::bigint, true, true, true, 51),
    ('Green Bowl Kitchen', 'Cold Drinks', 'Cold-Pressed Orange', 'Freshly pressed orange juice with no added sugar.', 149::bigint, true, true, true, 43)
) AS v(restaurant_name, category_name, food_name, description, price, available, vegetarian, seasonal, age_days)
JOIN restaurant r ON r.name = v.restaurant_name
JOIN category c ON c.restaurant_id = r.id AND c.name = v.category_name;

-- Food photography. URLs are remote demo assets and can later be replaced
-- through the owner menu editor.
INSERT INTO food_images (food_id, images)
SELECT f.id, v.image
FROM (VALUES
    ('Spice Route', 'Paneer Tikka', 'https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?auto=format&fit=crop&w=1000&q=80'),
    ('Spice Route', 'Tandoori Chicken', 'https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?auto=format&fit=crop&w=1000&q=80'),
    ('Spice Route', 'Butter Chicken', 'https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=1000&q=80'),
    ('Spice Route', 'Dal Makhani', 'https://images.unsplash.com/photo-1546833999-b9f581a1996d?auto=format&fit=crop&w=1000&q=80'),
    ('Spice Route', 'Vegetable Biryani', 'https://images.unsplash.com/photo-1589302168068-964664d93dc0?auto=format&fit=crop&w=1000&q=80'),
    ('Spice Route', 'Garlic Naan', 'https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=1000&q=80'),
    ('Spice Route', 'Gulab Jamun', 'https://images.unsplash.com/photo-1666190094760-4c6f4c3a0f58?auto=format&fit=crop&w=1000&q=80'),
    ('Spice Route', 'Mango Lassi', 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=1000&q=80'),
    ('Bombay Street Kitchen', 'Vada Pav', 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=1000&q=80'),
    ('Bombay Street Kitchen', 'Pav Bhaji', 'https://images.unsplash.com/photo-1596797038530-2c107229654b?auto=format&fit=crop&w=1000&q=80'),
    ('Bombay Street Kitchen', 'Misal Pav', 'https://images.unsplash.com/photo-1626132647523-66f5bf380027?auto=format&fit=crop&w=1000&q=80'),
    ('Bombay Street Kitchen', 'Dahi Puri', 'https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=1000&q=80'),
    ('Bombay Street Kitchen', 'Bombay Sandwich', 'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=1000&q=80'),
    ('Bombay Street Kitchen', 'Keema Pav', 'https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=1000&q=80'),
    ('Bombay Street Kitchen', 'Masala Chaas', 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=1000&q=80'),
    ('Bombay Street Kitchen', 'Rose Falooda', 'https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&w=1000&q=80'),
    ('Southern Stories', 'Idli Vada', 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=1000&q=80'),
    ('Southern Stories', 'Masala Dosa', 'https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=1000&q=80'),
    ('Southern Stories', 'Ghee Podi Dosa', 'https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=1000&q=80'),
    ('Southern Stories', 'Vegetable Uttapam', 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=1000&q=80'),
    ('Southern Stories', 'Lemon Rice', 'https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1000&q=80'),
    ('Southern Stories', 'Chettinad Chicken', 'https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=1000&q=80'),
    ('Southern Stories', 'Filter Coffee', 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=1000&q=80'),
    ('Southern Stories', 'Payasam', 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=1000&q=80'),
    ('Napoli Hearth', 'Margherita Pizza', 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=1000&q=80'),
    ('Napoli Hearth', 'Farmhouse Pizza', 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=1000&q=80'),
    ('Napoli Hearth', 'Pepperoni Pizza', 'https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&w=1000&q=80'),
    ('Napoli Hearth', 'Pasta Alfredo', 'https://images.unsplash.com/photo-1555949258-eb67b1ef0ceb?auto=format&fit=crop&w=1000&q=80'),
    ('Napoli Hearth', 'Penne Arrabbiata', 'https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=1000&q=80'),
    ('Napoli Hearth', 'Garlic Bread', 'https://images.unsplash.com/photo-1573140247632-f8fd74997d5c?auto=format&fit=crop&w=1000&q=80'),
    ('Napoli Hearth', 'Tiramisu', 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=1000&q=80'),
    ('Napoli Hearth', 'Lemon Iced Tea', 'https://images.unsplash.com/photo-1499638673689-79a0b5115d87?auto=format&fit=crop&w=1000&q=80'),
    ('Green Bowl Kitchen', 'Buddha Bowl', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1000&q=80'),
    ('Green Bowl Kitchen', 'Grilled Paneer Salad', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1000&q=80'),
    ('Green Bowl Kitchen', 'Chicken Protein Bowl', 'https://images.unsplash.com/photo-1546793665-c74683f339c1?auto=format&fit=crop&w=1000&q=80'),
    ('Green Bowl Kitchen', 'Avocado Toast', 'https://images.unsplash.com/photo-1541519227354-08fa5d50c44d?auto=format&fit=crop&w=1000&q=80'),
    ('Green Bowl Kitchen', 'Hummus Platter', 'https://images.unsplash.com/photo-1577805947697-89e18249d767?auto=format&fit=crop&w=1000&q=80'),
    ('Green Bowl Kitchen', 'Quinoa Khichdi', 'https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1000&q=80'),
    ('Green Bowl Kitchen', 'Berry Smoothie', 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?auto=format&fit=crop&w=1000&q=80'),
    ('Green Bowl Kitchen', 'Cold-Pressed Orange', 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=1000&q=80')
) AS v(restaurant_name, food_name, image)
JOIN restaurant r ON r.name = v.restaurant_name
JOIN food f ON f.restaurant_id = r.id AND f.name = v.food_name;

INSERT INTO food_ingredients (food_id, ingredients_id)
SELECT f.id, i.id
FROM (VALUES
    ('Spice Route', 'Paneer Tikka', 'Paneer'), ('Spice Route', 'Paneer Tikka', 'Hot'),
    ('Spice Route', 'Tandoori Chicken', 'Chicken'), ('Spice Route', 'Tandoori Chicken', 'Medium'),
    ('Spice Route', 'Butter Chicken', 'Extra Butter'), ('Spice Route', 'Butter Chicken', 'Medium'),
    ('Spice Route', 'Dal Makhani', 'Extra Butter'), ('Spice Route', 'Dal Makhani', 'Mild'),
    ('Bombay Street Kitchen', 'Vada Pav', 'Pav'), ('Bombay Street Kitchen', 'Vada Pav', 'Hot'),
    ('Bombay Street Kitchen', 'Pav Bhaji', 'Cheese'), ('Bombay Street Kitchen', 'Pav Bhaji', 'Onion'),
    ('Bombay Street Kitchen', 'Misal Pav', 'Sev'), ('Bombay Street Kitchen', 'Misal Pav', 'Hot'),
    ('Southern Stories', 'Masala Dosa', 'Rice Batter'), ('Southern Stories', 'Masala Dosa', 'Coconut Chutney'),
    ('Southern Stories', 'Ghee Podi Dosa', 'Ghee'), ('Southern Stories', 'Ghee Podi Dosa', 'Podi'),
    ('Southern Stories', 'Vegetable Uttapam', 'Ragi Batter'), ('Southern Stories', 'Vegetable Uttapam', 'Medium'),
    ('Napoli Hearth', 'Margherita Pizza', 'Mozzarella'), ('Napoli Hearth', 'Margherita Pizza', 'Olives'),
    ('Napoli Hearth', 'Farmhouse Pizza', 'Mushrooms'), ('Napoli Hearth', 'Farmhouse Pizza', 'Jalapenos'),
    ('Napoli Hearth', 'Pepperoni Pizza', 'Pepperoni'), ('Napoli Hearth', 'Pepperoni Pizza', 'Mozzarella'),
    ('Napoli Hearth', 'Pasta Alfredo', 'Parmesan'), ('Napoli Hearth', 'Garlic Bread', 'Garlic Dip'),
    ('Green Bowl Kitchen', 'Buddha Bowl', 'Tofu'), ('Green Bowl Kitchen', 'Buddha Bowl', 'Lemon Tahini'),
    ('Green Bowl Kitchen', 'Grilled Paneer Salad', 'Paneer'), ('Green Bowl Kitchen', 'Grilled Paneer Salad', 'Mint Yogurt'),
    ('Green Bowl Kitchen', 'Chicken Protein Bowl', 'Grilled Chicken'), ('Green Bowl Kitchen', 'Chicken Protein Bowl', 'Avocado'),
    ('Green Bowl Kitchen', 'Avocado Toast', 'Roasted Seeds'), ('Green Bowl Kitchen', 'Hummus Platter', 'Hummus')
) AS v(restaurant_name, food_name, ingredient_name)
JOIN restaurant r ON r.name = v.restaurant_name
JOIN food f ON f.restaurant_id = r.id AND f.name = v.food_name
JOIN ingredients_item i ON i.restaurant_id = r.id AND i.name = v.ingredient_name;

-- Saved customer address book entries. Historical orders below use separate
-- snapshot rows, so deleting these later will not alter order history.
WITH a AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Priya Sharma', 'B-204 Blue Ridge Apartments, Hinjawadi', 'Pune', 'Maharashtra', '411057', 'India')
    RETURNING id
)
INSERT INTO users_addresses (user_id, addresses_id)
SELECT u.id, a.id FROM users u CROSS JOIN a WHERE u.email = 'priya@dinehub.demo';

WITH a AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Rohan Verma', '15 Carter Road, Bandra West', 'Mumbai', 'Maharashtra', '400050', 'India')
    RETURNING id
)
INSERT INTO users_addresses (user_id, addresses_id)
SELECT u.id, a.id FROM users u CROSS JOIN a WHERE u.email = 'rohan@dinehub.demo';

WITH a AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Ananya Rao', '301 Lakeview Residency, Indiranagar', 'Bengaluru', 'Karnataka', '560038', 'India')
    RETURNING id
)
INSERT INTO users_addresses (user_id, addresses_id)
SELECT u.id, a.id FROM users u CROSS JOIN a WHERE u.email = 'ananya@dinehub.demo';

WITH a AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Kabir Singh', 'C-18 Green Park Extension', 'New Delhi', 'Delhi', '110016', 'India')
    RETURNING id
)
INSERT INTO users_addresses (user_id, addresses_id)
SELECT u.id, a.id FROM users u CROSS JOIN a WHERE u.email = 'kabir@dinehub.demo';

-- A few favourites make the customer profile feel populated.
INSERT INTO user_favourites (user_id, id, title, description, images)
SELECT u.id, r.id, r.name, r.description,
       ARRAY[(SELECT ri.images FROM restaurant_images ri WHERE ri.restaurant_id = r.id LIMIT 1)]::varchar(1000)[]
FROM users u
JOIN restaurant r ON r.name IN ('Spice Route', 'Southern Stories')
WHERE u.email = 'priya@dinehub.demo';

INSERT INTO user_favourites (user_id, id, title, description, images)
SELECT u.id, r.id, r.name, r.description,
       ARRAY[(SELECT ri.images FROM restaurant_images ri WHERE ri.restaurant_id = r.id LIMIT 1)]::varchar(1000)[]
FROM users u
JOIN restaurant r ON r.name IN ('Bombay Street Kitchen', 'Napoli Hearth')
WHERE u.email = 'rohan@dinehub.demo';

-- Historical demo orders with immutable item/address snapshots.
WITH delivery AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Priya Sharma', 'B-204 Blue Ridge Apartments, Hinjawadi', 'Pune', 'Maharashtra', '411057', 'India')
    RETURNING id
), new_order AS (
    INSERT INTO orders (
        customer_id, restaurant_id, delivery_address_id, total_amount, total_price,
        total_item, order_status, payment_status, stripe_session_id,
        stripe_payment_intent_id, paid_amount, payment_currency, paid_at, created_at
    )
    SELECT u.id, r.id, d.id, 487, 487, 3, 'DELIVERED', 'PAID',
           'seed_cs_spice_001', 'seed_pi_spice_001', 48700, 'inr',
           current_timestamp - interval '28 days', current_timestamp - interval '28 days 45 minutes'
    FROM users u CROSS JOIN restaurant r CROSS JOIN delivery d
    WHERE u.email = 'priya@dinehub.demo' AND r.name = 'Spice Route'
    RETURNING id
), i1 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Butter Chicken', 349, 1, 349, ARRAY['Medium']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Spice Route' AND f.name = 'Butter Chicken'
    RETURNING id
), i2 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Garlic Naan', 69, 2, 138, ARRAY[]::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Spice Route' AND f.name = 'Garlic Naan'
    RETURNING id
)
INSERT INTO orders_items (order_id, items_id)
SELECT o.id, i1.id FROM new_order o CROSS JOIN i1
UNION ALL SELECT o.id, i2.id FROM new_order o CROSS JOIN i2;

WITH delivery AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Rohan Verma', '15 Carter Road, Bandra West', 'Mumbai', 'Maharashtra', '400050', 'India')
    RETURNING id
), new_order AS (
    INSERT INTO orders (
        customer_id, restaurant_id, delivery_address_id, total_amount, total_price,
        total_item, order_status, payment_status, stripe_session_id,
        stripe_payment_intent_id, paid_amount, payment_currency, paid_at, created_at
    )
    SELECT u.id, r.id, d.id, 427, 427, 3, 'DELIVERED', 'PAID',
           'seed_cs_bombay_001', 'seed_pi_bombay_001', 42700, 'inr',
           current_timestamp - interval '21 days', current_timestamp - interval '21 days 35 minutes'
    FROM users u CROSS JOIN restaurant r CROSS JOIN delivery d
    WHERE u.email = 'rohan@dinehub.demo' AND r.name = 'Bombay Street Kitchen'
    RETURNING id
), i1 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Pav Bhaji', 179, 2, 358, ARRAY['Cheese', 'Onion']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Bombay Street Kitchen' AND f.name = 'Pav Bhaji'
    RETURNING id
), i2 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Masala Chaas', 69, 1, 69, ARRAY[]::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Bombay Street Kitchen' AND f.name = 'Masala Chaas'
    RETURNING id
)
INSERT INTO orders_items (order_id, items_id)
SELECT o.id, i1.id FROM new_order o CROSS JOIN i1
UNION ALL SELECT o.id, i2.id FROM new_order o CROSS JOIN i2;

WITH delivery AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Ananya Rao', '301 Lakeview Residency, Indiranagar', 'Bengaluru', 'Karnataka', '560038', 'India')
    RETURNING id
), new_order AS (
    INSERT INTO orders (
        customer_id, restaurant_id, delivery_address_id, total_amount, total_price,
        total_item, order_status, payment_status, stripe_session_id,
        stripe_payment_intent_id, paid_amount, payment_currency, paid_at, created_at
    )
    SELECT u.id, r.id, d.id, 416, 416, 4, 'DELIVERED', 'PAID',
           'seed_cs_south_001', 'seed_pi_south_001', 41600, 'inr',
           current_timestamp - interval '15 days', current_timestamp - interval '15 days 25 minutes'
    FROM users u CROSS JOIN restaurant r CROSS JOIN delivery d
    WHERE u.email = 'ananya@dinehub.demo' AND r.name = 'Southern Stories'
    RETURNING id
), i1 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Masala Dosa', 139, 2, 278, ARRAY['Rice Batter', 'Coconut Chutney']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Southern Stories' AND f.name = 'Masala Dosa'
    RETURNING id
), i2 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Filter Coffee', 69, 2, 138, ARRAY[]::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Southern Stories' AND f.name = 'Filter Coffee'
    RETURNING id
)
INSERT INTO orders_items (order_id, items_id)
SELECT o.id, i1.id FROM new_order o CROSS JOIN i1
UNION ALL SELECT o.id, i2.id FROM new_order o CROSS JOIN i2;

WITH delivery AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Kabir Singh', 'C-18 Green Park Extension', 'New Delhi', 'Delhi', '110016', 'India')
    RETURNING id
), new_order AS (
    INSERT INTO orders (
        customer_id, restaurant_id, delivery_address_id, total_amount, total_price,
        total_item, order_status, payment_status, stripe_session_id,
        stripe_payment_intent_id, paid_amount, payment_currency, paid_at, created_at
    )
    SELECT u.id, r.id, d.id, 498, 498, 2, 'DELIVERED', 'PAID',
           'seed_cs_napoli_001', 'seed_pi_napoli_001', 49800, 'inr',
           current_timestamp - interval '10 days', current_timestamp - interval '10 days 30 minutes'
    FROM users u CROSS JOIN restaurant r CROSS JOIN delivery d
    WHERE u.email = 'kabir@dinehub.demo' AND r.name = 'Napoli Hearth'
    RETURNING id
), i1 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Margherita Pizza', 299, 1, 299, ARRAY['Mozzarella']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Napoli Hearth' AND f.name = 'Margherita Pizza'
    RETURNING id
), i2 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Tiramisu', 199, 1, 199, ARRAY[]::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Napoli Hearth' AND f.name = 'Tiramisu'
    RETURNING id
)
INSERT INTO orders_items (order_id, items_id)
SELECT o.id, i1.id FROM new_order o CROSS JOIN i1
UNION ALL SELECT o.id, i2.id FROM new_order o CROSS JOIN i2;

WITH delivery AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Priya Sharma', 'B-204 Blue Ridge Apartments, Hinjawadi', 'Pune', 'Maharashtra', '411057', 'India')
    RETURNING id
), new_order AS (
    INSERT INTO orders (
        customer_id, restaurant_id, delivery_address_id, total_amount, total_price,
        total_item, order_status, payment_status, stripe_session_id,
        stripe_payment_intent_id, paid_amount, payment_currency, paid_at, created_at
    )
    SELECT u.id, r.id, d.id, 616, 616, 4, 'OUT_FOR_DELIVERY', 'PAID',
           'seed_cs_spice_002', 'seed_pi_spice_002', 61600, 'inr',
           current_timestamp - interval '2 hours', current_timestamp - interval '2 hours 12 minutes'
    FROM users u CROSS JOIN restaurant r CROSS JOIN delivery d
    WHERE u.email = 'priya@dinehub.demo' AND r.name = 'Spice Route'
    RETURNING id
), i1 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Paneer Tikka', 249, 1, 249, ARRAY['Paneer', 'Hot']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Spice Route' AND f.name = 'Paneer Tikka'
    RETURNING id
), i2 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Dal Makhani', 229, 1, 229, ARRAY['Extra Butter']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Spice Route' AND f.name = 'Dal Makhani'
    RETURNING id
), i3 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Garlic Naan', 69, 2, 138, ARRAY[]::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Spice Route' AND f.name = 'Garlic Naan'
    RETURNING id
)
INSERT INTO orders_items (order_id, items_id)
SELECT o.id, i1.id FROM new_order o CROSS JOIN i1
UNION ALL SELECT o.id, i2.id FROM new_order o CROSS JOIN i2
UNION ALL SELECT o.id, i3.id FROM new_order o CROSS JOIN i3;

WITH delivery AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Rohan Verma', '15 Carter Road, Bandra West', 'Mumbai', 'Maharashtra', '400050', 'India')
    RETURNING id
), new_order AS (
    INSERT INTO orders (
        customer_id, restaurant_id, delivery_address_id, total_amount, total_price,
        total_item, order_status, payment_status, stripe_session_id,
        stripe_payment_intent_id, paid_amount, payment_currency, paid_at, created_at
    )
    SELECT u.id, r.id, d.id, 118, 118, 2, 'CANCELLED', 'PAYMENT_CANCELLED',
           'seed_cs_cancelled_001', NULL, NULL, 'inr', NULL, current_timestamp - interval '6 days'
    FROM users u CROSS JOIN restaurant r CROSS JOIN delivery d
    WHERE u.email = 'rohan@dinehub.demo' AND r.name = 'Bombay Street Kitchen'
    RETURNING id
), i1 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Vada Pav', 59, 2, 118, ARRAY['Pav', 'Hot']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Bombay Street Kitchen' AND f.name = 'Vada Pav'
    RETURNING id
)
INSERT INTO orders_items (order_id, items_id)
SELECT o.id, i1.id FROM new_order o CROSS JOIN i1;

WITH delivery AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Kabir Singh', 'C-18 Green Park Extension', 'New Delhi', 'Delhi', '110016', 'India')
    RETURNING id
), new_order AS (
    INSERT INTO orders (
        customer_id, restaurant_id, delivery_address_id, total_amount, total_price,
        total_item, order_status, payment_status, stripe_session_id,
        stripe_payment_intent_id, paid_amount, payment_currency, paid_at, created_at
    )
    SELECT u.id, r.id, d.id, 329, 329, 1, 'PENDING', 'PENDING_PAYMENT',
           NULL, NULL, NULL, 'inr', NULL, current_timestamp - interval '30 minutes'
    FROM users u CROSS JOIN restaurant r CROSS JOIN delivery d
    WHERE u.email = 'kabir@dinehub.demo' AND r.name = 'Napoli Hearth'
    RETURNING id
), i1 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Pasta Alfredo', 329, 1, 329, ARRAY['Parmesan']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Napoli Hearth' AND f.name = 'Pasta Alfredo'
    RETURNING id
)
INSERT INTO orders_items (order_id, items_id)
SELECT o.id, i1.id FROM new_order o CROSS JOIN i1;

WITH delivery AS (
    INSERT INTO address (full_name, street_address, city, state_province, postal_code, country)
    VALUES ('Ananya Rao', '301 Lakeview Residency, Indiranagar', 'Bengaluru', 'Karnataka', '560038', 'India')
    RETURNING id
), new_order AS (
    INSERT INTO orders (
        customer_id, restaurant_id, delivery_address_id, total_amount, total_price,
        total_item, order_status, payment_status, stripe_session_id,
        stripe_payment_intent_id, paid_amount, payment_currency, paid_at, created_at
    )
    SELECT u.id, r.id, d.id, 287, 287, 3, 'PREPARING', 'PAID',
           'seed_cs_south_002', 'seed_pi_south_002', 28700, 'inr',
           current_timestamp - interval '48 minutes', current_timestamp - interval '55 minutes'
    FROM users u CROSS JOIN restaurant r CROSS JOIN delivery d
    WHERE u.email = 'ananya@dinehub.demo' AND r.name = 'Southern Stories'
    RETURNING id
), i1 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Idli Vada', 109, 2, 218, ARRAY['Coconut Chutney']::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Southern Stories' AND f.name = 'Idli Vada'
    RETURNING id
), i2 AS (
    INSERT INTO order_item (food_id, item_name, unit_price, quantity, total_price, ingredients)
    SELECT f.id, 'Filter Coffee', 69, 1, 69, ARRAY[]::varchar[]
    FROM food f JOIN restaurant r ON r.id = f.restaurant_id
    WHERE r.name = 'Southern Stories' AND f.name = 'Filter Coffee'
    RETURNING id
)
INSERT INTO orders_items (order_id, items_id)
SELECT o.id, i1.id FROM new_order o CROSS JOIN i1
UNION ALL SELECT o.id, i2.id FROM new_order o CROSS JOIN i2;

-- Align every pooled Hibernate sequence with the largest stored identifier.
-- The sequences increment by 50, so the next Hibernate allocation remains
-- safely above all existing and seeded records.
SELECT setval('users_seq', GREATEST(COALESCE((SELECT MAX(id) FROM users), 1), 1), true);
SELECT setval('restaurant_seq', GREATEST(COALESCE((SELECT MAX(id) FROM restaurant), 1), 1), true);
SELECT setval('category_seq', GREATEST(COALESCE((SELECT MAX(id) FROM category), 1), 1), true);
SELECT setval('food_seq', GREATEST(COALESCE((SELECT MAX(id) FROM food), 1), 1), true);
SELECT setval('ingredient_category_seq', GREATEST(COALESCE((SELECT MAX(id) FROM ingredient_category), 1), 1), true);
SELECT setval('ingredients_item_seq', GREATEST(COALESCE((SELECT MAX(id) FROM ingredients_item), 1), 1), true);
SELECT setval('cart_seq', GREATEST(COALESCE((SELECT MAX(id) FROM cart), 1), 1), true);
SELECT setval('cart_item_seq', GREATEST(COALESCE((SELECT MAX(id) FROM cart_item), 1), 1), true);

COMMIT;

-- Verification summary shown by the Neon SQL Editor.
SELECT 'users' AS entity, COUNT(*) AS total FROM users
UNION ALL SELECT 'restaurants', COUNT(*) FROM restaurant
UNION ALL SELECT 'categories', COUNT(*) FROM category
UNION ALL SELECT 'foods', COUNT(*) FROM food
UNION ALL SELECT 'ingredients', COUNT(*) FROM ingredients_item
UNION ALL SELECT 'orders', COUNT(*) FROM orders
ORDER BY entity;
