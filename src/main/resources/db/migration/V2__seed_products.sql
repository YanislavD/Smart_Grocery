insert into products (name, category, default_unit)
values
    ('Milk', 'Dairy', 'L'),
    ('Eggs', 'Dairy', 'PCS'),
    ('Bread', 'Bakery', 'PCS'),
    ('Rice', 'Grains', 'KG'),
    ('Pasta', 'Grains', 'KG'),
    ('Chicken Breast', 'Meat', 'KG'),
    ('Tomatoes', 'Vegetables', 'KG'),
    ('Onions', 'Vegetables', 'KG'),
    ('Apples', 'Fruits', 'KG'),
    ('Olive Oil', 'Pantry', 'ML')
on conflict do nothing;
