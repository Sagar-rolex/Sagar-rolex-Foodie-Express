package com.foodieexpress.config;

import com.foodieexpress.model.*;
import com.foodieexpress.repository.*;
import com.foodieexpress.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private FoodItemRepository foodItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private CustomerService customerService;
    @Autowired private FaqEntryRepository faqEntryRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed demo admin if not present
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@foodie.com")
                    .role(User.Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println("✓ Demo admin created (admin / admin123)");
        }

        // 2. Seed demo customer if not present
        if (userRepository.findByUsername("customer").isEmpty()) {
            User customerUser = User.builder()
                    .username("customer")
                    .password(passwordEncoder.encode("customer123"))
                    .email("customer@foodie.com")
                    .role(User.Role.CUSTOMER)
                    .build();
            User savedUser = userRepository.save(customerUser);

            if (customerRepository.findByUser(savedUser).isEmpty()) {
                customerService.createProfile(savedUser, "Sahil Salunkhe", "+91 9876543210", "101 Palm Grove, Marine Drive, Mumbai");
            }
            System.out.println("✓ Demo customer created (customer / customer123)");
        }

        // 3. Seed restaurants and expanded diverse food catalog idempotently
        seedCatalog();
        seedRaviChineseDishes();
        seedMissingImagePaths();
        seedFaqs();

        System.out.println("━━━ Foodie Express Ready ━━━");
        System.out.println("Admin login:     admin / admin123");
        System.out.println("Customer login:  customer / customer123");
    }

    private void seedCatalog() {
        List<RestaurantData> data = List.of(
                new RestaurantData("Spice Garden", "12 MG Road, Bandra, Mumbai", "Indian",
                        "Authentic North Indian, Biryanis, Street Food & Royal Desserts", 4.8, 30,
                        List.of(
                                // North Indian
                                new FoodData("Paneer Butter Masala", "Cottage cheese cubes simmered in a velvety, spiced cashew gravy", new BigDecimal("249.00"), "North Indian"),
                                new FoodData("Shahi Paneer", "Royal cottage cheese preparation in thick creamy tomato gravy", new BigDecimal("269.00"), "North Indian"),
                                new FoodData("Kadai Paneer", "Paneer tossed with bell peppers, onions, and freshly ground kadai masala", new BigDecimal("259.00"), "North Indian"),
                                new FoodData("Palak Paneer", "Soft cottage cheese in smooth spiced spinach puree", new BigDecimal("239.00"), "North Indian"),
                                new FoodData("Chole Bhature", "Spicy chickpea curry served with fluffy deep-fried bhature", new BigDecimal("189.00"), "North Indian"),
                                new FoodData("Dal Makhani", "Slow-cooked black lentils in butter and cream", new BigDecimal("199.00"), "North Indian"),
                                new FoodData("Dal Tadka", "Yellow lentils tempered with garlic, cumin, and ghee", new BigDecimal("169.00"), "North Indian"),
                                new FoodData("Rajma Chawal", "Comforting red kidney bean curry served with steamed basmati rice", new BigDecimal("179.00"), "North Indian"),
                                new FoodData("Chole Kulche", "Spiced chickpea curry with soft stuffed kulcha bread", new BigDecimal("179.00"), "North Indian"),
                                new FoodData("Aloo Paratha", "Whole wheat flatbread stuffed with spiced potatoes and baked with butter", new BigDecimal("129.00"), "North Indian"),
                                new FoodData("Gobi Paratha", "Flatbread stuffed with spiced cauliflower filling", new BigDecimal("139.00"), "North Indian"),
                                new FoodData("Paneer Paratha", "Stuffed flatbread with spiced grated paneer", new BigDecimal("159.00"), "North Indian"),
                                new FoodData("Butter Naan", "Soft tandoori flatbread brushed with butter", new BigDecimal("55.00"), "North Indian"),
                                new FoodData("Garlic Naan", "Tandoori flatbread topped with roasted garlic and butter", new BigDecimal("65.00"), "North Indian"),
                                new FoodData("Tandoori Roti", "Whole wheat bread baked in clay tandoor", new BigDecimal("35.00"), "North Indian"),
                                new FoodData("Paneer Tikka", "Clay-oven roasted cottage cheese with bell peppers and spices", new BigDecimal("229.00"), "North Indian"),
                                new FoodData("Tandoori Chicken", "Juicy bone-in chicken marinated in yogurt and fiery tandoori spices", new BigDecimal("349.00"), "North Indian"),
                                new FoodData("Chicken Tikka Masala", "Tender chicken tikka in a rich, spicy tomato-onion gravy", new BigDecimal("329.00"), "North Indian"),
                                new FoodData("Butter Chicken", "Tender chicken in a rich, buttery tomato gravy with aromatic spices", new BigDecimal("299.00"), "North Indian"),
                                new FoodData("Amritsari Kulcha", "Crispy stuffed potato-onion kulcha served with chole", new BigDecimal("199.00"), "North Indian"),

                                // Biryani & Rice
                                new FoodData("Hyderabadi Veg Biryani", "Fragrant basmati rice cooked with assorted vegetables and authentic dum spices", new BigDecimal("249.00"), "Biryani"),
                                new FoodData("Hyderabadi Chicken Biryani", "Classic dum-cooked chicken biryani with saffron and aromatic herbs", new BigDecimal("329.00"), "Biryani"),
                                new FoodData("Lucknowi Biryani", "Awadhi style delicate slow-cooked fragrant chicken biryani", new BigDecimal("349.00"), "Biryani"),
                                new FoodData("Kolkata Biryani", "Flavourful aromatic rice with tender chicken and spiced boiled potato", new BigDecimal("299.00"), "Biryani"),
                                new FoodData("Egg Biryani", "Spiced rice layered with boiled eggs and fried onions", new BigDecimal("229.00"), "Biryani"),
                                new FoodData("Paneer Biryani", "Basmati rice layered with spiced paneer cubes and herbs", new BigDecimal("279.00"), "Biryani"),
                                new FoodData("Chicken Dum Biryani", "Traditional layered clay-pot chicken biryani", new BigDecimal("339.00"), "Biryani"),
                                new FoodData("Mutton Biryani", "Tender succulent mutton pieces slow-cooked with aged basmati rice", new BigDecimal("449.00"), "Biryani"),
                                new FoodData("Jeera Rice", "Basmati rice tempered with cumin seeds and ghee", new BigDecimal("129.00"), "Biryani"),
                                new FoodData("Dal Khichdi", "Comforting blend of lentils and rice tempered with ghee and spices", new BigDecimal("159.00"), "Biryani"),

                                // Street Food & Chaat
                                new FoodData("Vada Pav", "Spiced potato fritter stuffed in a soft bun with chutneys", new BigDecimal("59.00"), "Street Food"),
                                new FoodData("Pav Bhaji", "Spiced mashed vegetable curry served with butter-toasted pav", new BigDecimal("169.00"), "Street Food"),
                                new FoodData("Pani Puri", "Crispy hollow puris filled with tangy spiced mint water and potatoes", new BigDecimal("79.00"), "Street Food"),
                                new FoodData("Sev Puri", "Crisp puris topped with potatoes, chutneys, and crunchy sev", new BigDecimal("99.00"), "Street Food"),
                                new FoodData("Bhel Puri", "Puffed rice tossed with crunchy sev, potatoes, onions, and chutneys", new BigDecimal("89.00"), "Street Food"),
                                new FoodData("Samosa (2 pcs)", "Crispy pastry filled with spiced potatoes and peas", new BigDecimal("59.00"), "Street Food"),
                                new FoodData("Kachori", "Crispy fried pastry stuffed with spiced lentil filling", new BigDecimal("69.00"), "Street Food"),
                                new FoodData("Dhokla", "Fluffy, savory fermented gram flour cake tempered with mustard seeds", new BigDecimal("89.00"), "Street Food"),
                                new FoodData("Khandvi", "Rolled gram flour savory snack tempered with mustard and coconut", new BigDecimal("99.00"), "Street Food"),
                                new FoodData("Poha", "Flattened rice tempered with mustard seeds, curry leaves, and peanuts", new BigDecimal("79.00"), "Street Food"),

                                // Desserts & Sweets
                                new FoodData("Gulab Jamun (2 pcs)", "Warm milk-solid dumplings soaked in saffron sugar syrup", new BigDecimal("89.00"), "Desserts"),
                                new FoodData("Rasmalai (2 pcs)", "Soft cottage cheese patties immersed in chilled sweetened thickened milk", new BigDecimal("119.00"), "Desserts"),
                                new FoodData("Gajar Halwa", "Rich traditional carrot pudding cooked in ghee, milk, and nuts", new BigDecimal("129.00"), "Desserts"),
                                new FoodData("Kheer", "Creamy traditional rice pudding infused with cardamom and dry fruits", new BigDecimal("99.00"), "Desserts"),
                                new FoodData("Jalebi", "Crispy deep-fried pretzel spirals soaked in warm sugar syrup", new BigDecimal("89.00"), "Desserts"),
                                new FoodData("Rasgulla (2 pcs)", "Spongy cottage cheese balls dipped in light sugar syrup", new BigDecimal("89.00"), "Desserts"),

                                // Beverages
                                new FoodData("Masala Chai", "Traditional Indian spiced milk tea with ginger and cardamom", new BigDecimal("49.00"), "Beverages"),
                                new FoodData("Mango Lassi", "Refreshing yogurt smoothie blended with sweet Alphonso mango pulp", new BigDecimal("119.00"), "Beverages"),
                                new FoodData("Sweet Lassi", "Chilled traditional sweetened yogurt drink", new BigDecimal("89.00"), "Beverages"),
                                new FoodData("Buttermilk (Chaas)", "Refreshing spiced yogurt cooler with roasted cumin and coriander", new BigDecimal("69.00"), "Beverages"),
                                new FoodData("Fresh Lime Soda", "Sparkling soda with fresh lime juice, mint, and choice of sweet/salt", new BigDecimal("79.00"), "Beverages")
                        )),

                new RestaurantData("Dakshin Rasoi", "45 T. Nagar, Chennai", "South Indian",
                        "Authentic Dosa, Crispy Vada, Filter Coffee & Traditional Regional Delicacies", 4.7, 25,
                        List.of(
                                new FoodData("Masala Dosa", "Crispy fermented crepe stuffed with spiced potato masala", new BigDecimal("179.00"), "South Indian"),
                                new FoodData("Mysore Masala Dosa", "Crispy dosa smeared with red garlic chutney and potato filling", new BigDecimal("199.00"), "South Indian"),
                                new FoodData("Plain Dosa", "Traditional golden crispy crepe served with chutney and sambar", new BigDecimal("139.00"), "South Indian"),
                                new FoodData("Rava Dosa", "Lacy, crisp semolina crepe laced with cumin and pepper", new BigDecimal("189.00"), "South Indian"),
                                new FoodData("Set Dosa", "Soft, spongy set of three mini pancakes served with veg kurma", new BigDecimal("169.00"), "South Indian"),
                                new FoodData("Onion Uttapam", "Thick savory pancake topped with finely chopped onions and green chillies", new BigDecimal("159.00"), "South Indian"),
                                new FoodData("Idli Sambar (3 pcs)", "Steamed fluffy rice cakes served with hot sambar and coconut chutney", new BigDecimal("119.00"), "South Indian"),
                                new FoodData("Medu Vada (2 pcs)", "Crispy golden fried lentil donuts with soft interior", new BigDecimal("109.00"), "South Indian"),
                                new FoodData("Idli Vada Combo", "2 Idlis and 1 Medu Vada served with sambar and chutneys", new BigDecimal("139.00"), "South Indian"),
                                new FoodData("Podi Idli", "Mini idlis tossed in spicy milagai podi and ghee", new BigDecimal("149.00"), "South Indian"),
                                new FoodData("Pongal", "Comforting South Indian rice and lentil dish tempered with pepper and ghee", new BigDecimal("149.00"), "South Indian"),
                                new FoodData("Lemon Rice", "Tangy turmeric-infused rice with peanuts and curry leaves", new BigDecimal("129.00"), "South Indian"),
                                new FoodData("Curd Rice", "Cooling yogurt rice tempered with mustard seeds and green chillies", new BigDecimal("119.00"), "South Indian"),
                                new FoodData("Appam with Vegetable Stew", "Lacy fermented rice hopper served with creamy coconut vegetable stew", new BigDecimal("219.00"), "South Indian"),
                                new FoodData("Kerala Parotta", "Flaky, layered multi-layered flatbread", new BigDecimal("59.00"), "South Indian"),
                                new FoodData("Filter Coffee", "Authentic South Indian frothy decoction coffee", new BigDecimal("69.00"), "Beverages")
                        )),

                new RestaurantData("Dragon Wok", "24 Park Street, Kolkata", "Chinese",
                        "Authentic Wok Tossed Noodles, Fried Rice, Manchurian, Momos & Soups", 4.6, 35,
                        List.of(
                                new FoodData("Veg Hakka Noodles", "Wok-tossed noodles with crunchy vegetables and soy sauce", new BigDecimal("199.00"), "Chinese"),
                                new FoodData("Chicken Hakka Noodles", "Stir-fried noodles with chicken, vegetables, and savory sauces", new BigDecimal("249.00"), "Chinese"),
                                new FoodData("Schezwan Noodles", "Spicy stir-fried noodles tossed in pungent schezwan chili sauce", new BigDecimal("229.00"), "Chinese"),
                                new FoodData("Veg Fried Rice", "Wok-fried aromatic rice with diced vegetables and soy seasoning", new BigDecimal("189.00"), "Chinese"),
                                new FoodData("Chicken Fried Rice", "Classic fried rice with tender chicken chunks and egg", new BigDecimal("249.00"), "Chinese"),
                                new FoodData("Schezwan Fried Rice", "Spicy fried rice tossed in schezwan pepper sauce", new BigDecimal("259.00"), "Chinese"),
                                new FoodData("Veg Manchurian", "Crispy vegetable dumplings in tangy Manchurian gravy", new BigDecimal("229.00"), "Chinese"),
                                new FoodData("Gobi Manchurian", "Batter-fried cauliflower florets tossed in spicy sweet-sour sauce", new BigDecimal("219.00"), "Chinese"),
                                new FoodData("Chilli Paneer", "Crispy paneer cubes tossed with bell peppers, onions, and soy chilli sauce", new BigDecimal("269.00"), "Chinese"),
                                new FoodData("Chilli Chicken", "Tender chicken pieces tossed with green chillies and dark soy sauce", new BigDecimal("289.00"), "Chinese"),
                                new FoodData("Spring Rolls (4 pcs)", "Golden fried crispy rolls stuffed with seasoned vegetables", new BigDecimal("169.00"), "Chinese"),
                                new FoodData("Veg Momos (6 pcs)", "Steamed dumplings stuffed with finely minced vegetables", new BigDecimal("159.00"), "Chinese"),
                                new FoodData("Chicken Momos (6 pcs)", "Juicy steamed chicken dumplings with fiery dip", new BigDecimal("199.00"), "Chinese"),
                                new FoodData("Hot and Sour Soup", "Spicy and tangy broth with mushrooms, bamboo shoots, and tofu", new BigDecimal("139.00"), "Chinese"),
                                new FoodData("Sweet Corn Soup", "Comforting golden corn soup with fresh herbs", new BigDecimal("129.00"), "Chinese"),
                                new FoodData("Manchow Soup", "Dark flavorful soup topped with crispy fried noodles", new BigDecimal("149.00"), "Chinese"),
                                new FoodData("Crispy Corn", "Golden sweet corn kernels tossed in salt, pepper, and spring onions", new BigDecimal("179.00"), "Chinese")
                        )),

                new RestaurantData("Burger Barn", "8 Anna Nagar, Chennai", "Burger & Fast Food",
                        "Juicy Smash Burgers, Artisanal Pizzas, Wraps, Loaded Fries & Shakes", 4.5, 30,
                        List.of(
                                new FoodData("Classic Veg Burger", "Crispy potato-veg patty with lettuce, tomatoes, and mayo", new BigDecimal("159.00"), "Burger"),
                                new FoodData("Crispy Chicken Burger", "Fried chicken breast with coleslaw, pickles, and creamy sauce", new BigDecimal("249.00"), "Burger"),
                                new FoodData("Paneer Burger", "Spiced grilled paneer patty with lettuce and mint mayo", new BigDecimal("219.00"), "Burger"),
                                new FoodData("Cheese Burger", "Double smash beef/veg patty loaded with melted cheddar", new BigDecimal("239.00"), "Burger"),
                                new FoodData("Margherita Pizza", "Classic wood-fired style pizza with mozzarella and basil", new BigDecimal("329.00"), "Pizza"),
                                new FoodData("Farmhouse Pizza", "Loaded with bell peppers, mushrooms, onions, and tomatoes", new BigDecimal("399.00"), "Pizza"),
                                new FoodData("Paneer Tikka Pizza", "Fusion pizza topped with tandoori paneer and onions", new BigDecimal("419.00"), "Pizza"),
                                new FoodData("Veggie Supreme Pizza", "Extravaganza of veggies, olives, and double cheese", new BigDecimal("449.00"), "Pizza"),
                                new FoodData("Club Sandwich", "Triple-decker sandwich with chicken, lettuce, tomato, and cheese", new BigDecimal("229.00"), "Sandwich"),
                                new FoodData("Grilled Cheese Sandwich", "Classic buttery toast with gooey melted cheese blend", new BigDecimal("149.00"), "Sandwich"),
                                new FoodData("Bombay Sandwich", "Street-style spiced potato, beetroot, chutney, and cheese sandwich", new BigDecimal("159.00"), "Sandwich"),
                                new FoodData("Garlic Bread", "Crispy toasted bread with garlic herb butter", new BigDecimal("129.00"), "Starter"),
                                new FoodData("Cheesy Garlic Bread", "Toasted garlic bread loaded with mozzarella cheese", new BigDecimal("169.00"), "Starter"),
                                new FoodData("Loaded Fries", "Crispy fries topped with melted cheese, jalapenos, and sauce", new BigDecimal("179.00"), "Starter"),
                                new FoodData("Peri-Peri Fries", "Crispy golden fries tossed in fiery peri-peri seasoning", new BigDecimal("149.00"), "Starter"),
                                new FoodData("Cold Coffee", "Chilled blended coffee with milk and vanilla ice cream", new BigDecimal("139.00"), "Beverages"),
                                new FoodData("Chocolate Milkshake", "Rich chocolate shake topped with whipped cream", new BigDecimal("159.00"), "Beverages")
                        )),

                new RestaurantData("Taco Fiesta", "15 Indiranagar, Bengaluru", "Mexican & Wraps",
                        "Zesty Tacos, Burritos, Quesadillas, Frankie Rolls & Mediterranean Bowls", 4.7, 35,
                        List.of(
                                new FoodData("Veg Frankie", "Spiced vegetable roll with onions and tangy sauces", new BigDecimal("139.00"), "Rolls & Wraps"),
                                new FoodData("Paneer Frankie", "Roll stuffed with spiced paneer cubes and peppers", new BigDecimal("179.00"), "Rolls & Wraps"),
                                new FoodData("Chicken Frankie", "Juicy chicken roll with signature spices and onions", new BigDecimal("199.00"), "Rolls & Wraps"),
                                new FoodData("Egg Roll", "Roll with double egg coating and spiced onion filling", new BigDecimal("129.00"), "Rolls & Wraps"),
                                new FoodData("Chicken Kathi Roll", "Kolkata style wrap with succulent tandoori chicken", new BigDecimal("229.00"), "Rolls & Wraps"),
                                new FoodData("Paneer Wrap", "Tortilla wrap with grilled paneer, lettuce, and chipotle mayo", new BigDecimal("199.00"), "Rolls & Wraps"),
                                new FoodData("Chicken Shawarma", "Middle eastern wrap with shredded chicken, garlic sauce, and pickles", new BigDecimal("219.00"), "Rolls & Wraps"),
                                new FoodData("Chipotle Chicken Burrito", "Rice, beans, grilled chicken, and salsa wrapped in tortilla", new BigDecimal("289.00"), "Mexican"),
                                new FoodData("Cheesy Quesadilla", "Toasted tortilla stuffed with cheese and peppers", new BigDecimal("229.00"), "Mexican"),
                                new FoodData("Hummus with Pita", "Creamy chickpea hummus served with warm pita bread and olive oil", new BigDecimal("199.00"), "Mediterranean"),
                                new FoodData("Falafel Bowl", "Crispy falafel balls with salad, tahini, and hummus", new BigDecimal("259.00"), "Mediterranean")
                        )),

                new RestaurantData("Healthy Bowl & Cafe", "7 Juhu Road, Mumbai", "Healthy & Breakfast",
                        "Nutritious Salads, Fruit Bowls, Oats, Grilled Protein Meals & Fresh Juices", 4.8, 25,
                        List.of(
                                new FoodData("Sprouts Salad", "Protein-rich sprouted beans tossed with cucumber, tomatoes, and lemon", new BigDecimal("149.00"), "Healthy"),
                                new FoodData("Fruit Bowl", "Assorted seasonal fresh fruits with honey and mint drizzle", new BigDecimal("179.00"), "Healthy"),
                                new FoodData("Greek Salad", "Crisp lettuce, cucumbers, olives, feta cheese, and olive oil dressing", new BigDecimal("229.00"), "Healthy"),
                                new FoodData("Grilled Paneer Bowl", "Herb-grilled paneer with quinoa, roasted veggies, and vinaigrette", new BigDecimal("279.00"), "Healthy"),
                                new FoodData("Grilled Chicken Bowl", "Juicy grilled chicken breast with brown rice and steamed broccoli", new BigDecimal("319.00"), "Healthy"),
                                new FoodData("Vegetable Oats", "Savory warm oatmeal cooked with carrots, peas, and mild spices", new BigDecimal("129.00"), "Breakfast"),
                                new FoodData("Poha", "Light flattened rice breakfast with peanuts and turmeric", new BigDecimal("89.00"), "Breakfast"),
                                new FoodData("Upma", "Traditional semolina breakfast cooked with mustard seeds and curry leaves", new BigDecimal("89.00"), "Breakfast"),
                                new FoodData("Fresh Orange Juice", "100% pure squeezed sweet orange juice without added sugar", new BigDecimal("129.00"), "Beverages"),
                                new FoodData("Coconut Water", "Natural tender coconut water, ice cold and refreshing", new BigDecimal("79.00"), "Beverages")
                        ))
        );

        for (RestaurantData rd : data) {
            Restaurant restaurant = restaurantRepository.findByName(rd.name)
                    .orElseGet(() -> Restaurant.builder()
                            .name(rd.name)
                            .address(rd.address)
                            .cuisineType(rd.cuisineType)
                            .description(rd.description)
                            .rating(rd.rating)
                            .deliveryTime(rd.deliveryTime)
                            .active(true)
                            .build());

            if (restaurant.getId() == null) {
                restaurant = restaurantRepository.save(restaurant);
            } else {
                // Update basic details if needed
                restaurant.setAddress(rd.address);
                restaurant.setCuisineType(rd.cuisineType);
                restaurant.setDescription(rd.description);
                restaurant.setRating(rd.rating);
                restaurant.setDeliveryTime(rd.deliveryTime);
                restaurant = restaurantRepository.save(restaurant);
            }

            for (FoodData fd : rd.items) {
                // Idempotent check by restaurant and name
                boolean exists = foodItemRepository.findByRestaurantAndNameIgnoreCase(restaurant, fd.name).isPresent();

                if (!exists) {
                    FoodItem item = FoodItem.builder()
                            .restaurant(restaurant)
                            .name(fd.name)
                            .description(fd.description)
                            .price(fd.price)
                            .category(fd.category)
                            .available(true)
                            .build();
                    foodItemRepository.save(item);
                }
            }
        }
        System.out.println("✓ Seeded expanded diverse food catalog successfully with over 100+ dishes!");
    }

    /** Adds the Chinese menu to an existing Ravi Chinese Corner without duplicating dishes. */
    private void seedRaviChineseDishes() {
        restaurantRepository.findByName("Ravi Chinese Corner").ifPresent(restaurant -> {
            List<FoodData> dishes = List.of(
                    new FoodData("Veg Hakka Noodles", "Wok-tossed noodles with crunchy vegetables and savory soy sauce", new BigDecimal("199.00"), "Chinese"),
                    new FoodData("Chicken Hakka Noodles", "Stir-fried noodles with tender chicken and vegetables", new BigDecimal("249.00"), "Chinese"),
                    new FoodData("Schezwan Noodles", "Spicy wok-fried noodles with bold Schezwan chili sauce", new BigDecimal("229.00"), "Chinese"),
                    new FoodData("Veg Fried Rice", "Wok-fried rice with diced vegetables and light soy seasoning", new BigDecimal("189.00"), "Chinese"),
                    new FoodData("Chicken Fried Rice", "Wok-fried rice with tender chicken and egg", new BigDecimal("249.00"), "Chinese"),
                    new FoodData("Veg Manchurian", "Crispy vegetable dumplings tossed in tangy Manchurian sauce", new BigDecimal("229.00"), "Chinese"),
                    new FoodData("Chilli Paneer", "Crispy paneer with bell peppers and spicy soy-chilli sauce", new BigDecimal("269.00"), "Chinese"),
                    new FoodData("Spring Rolls (4 pcs)", "Golden crisp rolls filled with seasoned vegetables", new BigDecimal("169.00"), "Chinese"),
                    new FoodData("Veg Momos (6 pcs)", "Steamed vegetable dumplings served with a fiery dip", new BigDecimal("159.00"), "Chinese"),
                    new FoodData("Hot and Sour Soup", "Spicy, tangy Chinese soup with vegetables and mushrooms", new BigDecimal("139.00"), "Chinese")
            );

            for (FoodData dish : dishes) {
                if (foodItemRepository.findByRestaurantAndNameIgnoreCase(restaurant, dish.name).isEmpty()) {
                    foodItemRepository.save(FoodItem.builder()
                            .restaurant(restaurant)
                            .name(dish.name)
                            .description(dish.description)
                            .price(dish.price)
                            .category(dish.category)
                            .available(true)
                            .build());
                }
            }
        });
    }

    /** Backfills only missing paths, preserving existing paths and all other record data. */
    private void seedMissingImagePaths() {
        for (Restaurant restaurant : restaurantRepository.findAll()) {
            if (restaurant.getImagePath() == null || restaurant.getImagePath().isBlank()) {
                restaurant.setImagePath(ImageDefaults.restaurant(restaurant));
                restaurantRepository.save(restaurant);
            }
        }
        for (FoodItem item : foodItemRepository.findAll()) {
            String imagePath = item.getImagePath();
            String defaultImagePath = ImageDefaults.food(item);
            boolean isLegacyGenericFoodImage = imagePath != null && Set.of(
                    "/images/burger.jpg", "/images/pizza.jpg", "/images/shared-meal.jpg", "/images/coffee.jpg",
                    "/images/dessert.jpg", "/images/indian-thali.jpg", "/images/noodles.jpg", "/images/sushi.jpg",
                    "/images/curry.jpg", "/images/dosa.jpg", "/images/salad.jpg", "/images/taco.jpg").contains(imagePath)
                    && defaultImagePath.startsWith("/images/food-banners/");
            if (imagePath == null || imagePath.isBlank() || isLegacyGenericFoodImage) {
                item.setImagePath(defaultImagePath);
                foodItemRepository.save(item);
            }
        }
        System.out.println("Backfilled missing local restaurant and food image paths");
    }

    private void seedFaqs() {
        if (faqEntryRepository.count() > 0) return;
        List<FaqEntry> entries = List.of(
                faq(FaqEntry.Category.ORDERS, "Where can I see my order status?", "Open My Orders from your account to see the current status and order details."),
                faq(FaqEntry.Category.ORDERS, "Can I change my order after placing it?", "Please contact support as soon as possible and include your order number. Changes depend on the preparation status."),
                faq(FaqEntry.Category.PAYMENTS, "Which payment method is available?", "Checkout uses Razorpay for secure online payment. The payment amount is calculated by Foodie Express from your order."),
                faq(FaqEntry.Category.PAYMENTS, "My payment failed. Was I charged?", "Check your order and bank or payment app status. If money was debited but the order is not confirmed, open a support ticket with the order number and payment reference."),
                faq(FaqEntry.Category.DELIVERY, "How do I find delivery updates?", "Open My Orders and select the order to see its latest status."),
                faq(FaqEntry.Category.DELIVERY, "Can I change the delivery address?", "Contact support with your order number as soon as possible. Address changes may not be possible once delivery is underway."),
                faq(FaqEntry.Category.CANCELLATIONS, "Can I cancel an order?", "A pending unpaid order can be cancelled from its order details if a payment attempt has not started. If payment was attempted or you need help with another order, open a support ticket."),
                faq(FaqEntry.Category.CANCELLATIONS, "What if I need to cancel a paid order?", "Contact support with the order number. The support team will review the order and payment status."),
                faq(FaqEntry.Category.COUPONS, "How do I apply a coupon?", "Enter the coupon code in your cart or checkout and select Apply. The code must be active and meet its minimum order and usage conditions."),
                faq(FaqEntry.Category.COUPONS, "Why was my coupon rejected?", "A coupon may be expired, inactive, limited to a minimum order value, or have reached its total or per-customer usage limit."),
                faq(FaqEntry.Category.REFUNDS, "How do I request a refund?", "Create a support ticket under Refunds and include your order number and payment reference. The team will review your request and update you in the ticket."),
                faq(FaqEntry.Category.REFUNDS, "Where can I check a refund update?", "Open your support ticket to see replies and status updates. Any payment-provider processing time depends on your bank or payment method.")
        );
        faqEntryRepository.saveAll(entries);
    }

    private FaqEntry faq(FaqEntry.Category category, String question, String answer) {
        return FaqEntry.builder().category(category).question(question).answer(answer).active(true).build();
    }

    record RestaurantData(String name, String address, String cuisineType, String description, Double rating, Integer deliveryTime, List<FoodData> items) {}
    record FoodData(String name, String description, BigDecimal price, String category) {}
}
