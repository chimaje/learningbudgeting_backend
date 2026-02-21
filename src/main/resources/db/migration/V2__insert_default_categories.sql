-- Insert default categories
INSERT INTO categories (name, description, icon_name, color, is_default) VALUES
('Groceries', 'Groceries, food delivery', 'utensils', '#FF6B6B', true),
('Transportation', 'Gas, public transit, ride-sharing', 'car', '#4ECDC4', true),
('Shopping', 'Clothing, electronics, general shopping', 'shopping-bag', '#95E1D3', true),
('Bills & Utilities', 'Rent, electricity, water, internet', 'file-text', '#AA96DA', true),
('Personal Care', 'Haircuts, cosmetics, spa', 'scissors', '#FFCCCC', true),
('Savings', 'Savings transfers, investments', 'piggy-bank', '#90EE90', true),
('Other', 'Miscellaneous expenses', 'more-horizontal', '#DDDDDD', true);