package com.woocommerce.android.ui.login.storecreation.profiler

import com.google.gson.Gson
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StoreProfilerRepository @Inject constructor(
    private val gson: Gson,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    suspend fun fetchProfilerOptions(): ProfilerOptions = withContext(coroutineDispatchers.io) {
        gson.fromJson(PROFILER_OPTIONS_JSON, ProfilerOptions::class.java)
    }
}

private const val PROFILER_OPTIONS_JSON = """{
  "aboutMerchant": [
    {
      "id": 1,
      "value": "I'm just starting my business",
      "heading": "I'm just starting my business",
      "description": "",
      "tracks": "im_just_starting_my_business"
    },
    {
      "id": 2,
      "value": "I'm already selling, but not online",
      "heading": "I'm already selling, but not online",
      "description": "",
      "tracks": "im_already_selling_but_not_online"
    },
    {
      "id": 3,
      "value": "I'm already selling online",
      "heading": "I'm already selling online",
      "description": "",
      "tracks": "im_already_selling_online",
      "platforms": [
        { "value": "amazon", "label": "Amazon" },
        { "value": "big-cartel", "label": "Big Cartel" },
        { "value": "big-commerce", "label": "Big Commerce" },
        { "value": "ebay", "label": "Ebay" },
        { "value": "etsy", "label": "Etsy" },
        { "value": "facebook-marketplace", "label": "Facebook Marketplace" },
        { "value": "google-shopping", "label": "Google Shopping" },
        { "value": "pinterest", "label": "Pinterest" },
        { "value": "shopify", "label": "Shopify" },
        { "value": "square", "label": "Square" },
        { "value": "squarespace", "label": "Squarespace" },
        { "value": "wix", "label": "Wix" },
        { "value": "wordPress", "label": "WordPress" }
      ]
    }
  ],
  "industries": [
    {
      "id": 0,
      "label": "Boat Sales",
      "key": "boat_sales",
      "tracks": "automotive"
    },
    {
      "id": 1,
      "label": "Car Washes",
      "key": "car_washes",
      "tracks": "automotive"
    },
    {
      "id": 2,
      "label": "Fuel Dispensers",
      "key": "fuel_dispensers",
      "tracks": "automotive"
    },
    {
      "id": 3,
      "label": "Towing Services",
      "key": "towing_services",
      "tracks": "automotive"
    },
    {
      "id": 4,
      "label": "Truck Stop",
      "key": "truck_stop",
      "tracks": "automotive"
    },
    {
      "id": 5,
      "label": "A C And Heating Contractors",
      "key": "a_c_and_heating_contractors",
      "tracks": "construction_industrial"
    },
    {
      "id": 6,
      "label": "Carpentry Contractors",
      "key": "carpentry_contractors",
      "tracks": "construction_industrial"
    },
    {
      "id": 7,
      "label": "Electrical Contractors",
      "key": "electrical_contractors",
      "tracks": "construction_industrial"
    },
    {
      "id": 8,
      "label": "General Contractors",
      "key": "general_contractors",
      "tracks": "construction_industrial"
    },
    {
      "id": 9,
      "label": "Other Building Services",
      "key": "other_building_services",
      "tracks": "construction_industrial"
    },
    {
      "id": 10,
      "label": "Special Trade Contractors",
      "key": "special_trade_contractors",
      "tracks": "construction_industrial"
    },
    {
      "id": 11,
      "label": "Telecom Equipment",
      "key": "telecom_equipment",
      "tracks": "construction_industrial"
    },
    {
      "id": 12,
      "label": "Telecom Services",
      "key": "telecom_services",
      "tracks": "construction_industrial"
    },
    {
      "id": 13,
      "label": "Apps",
      "key": "apps",
      "tracks": "digital_products"
    },
    {
      "id": 14,
      "label": "Blogs And Written Content",
      "key": "blogs_and_written_content",
      "tracks": "digital_products"
    },
    {
      "id": 15,
      "label": "Books",
      "key": "books",
      "tracks": "digital_products"
    },
    {
      "id": 16,
      "label": "Games",
      "key": "games",
      "tracks": "digital_products"
    },
    {
      "id": 17,
      "label": "Music Or Other Media",
      "key": "music_or_other_media",
      "tracks": "digital_products"
    },
    {
      "id": 18,
      "label": "Other Digital Goods",
      "key": "other_digital_goods",
      "tracks": "digital_products"
    },
    {
      "id": 19,
      "label": "Software As A Service",
      "key": "software_as_a_service",
      "tracks": "digital_products"
    },
    {
      "id": 20,
      "label": "Business And Secretarial Schools",
      "key": "business_and_secretarial_schools",
      "tracks": "education_learning"
    },
    {
      "id": 21,
      "label": "Child Care Services",
      "key": "child_care_services",
      "tracks": "education_learning"
    },
    {
      "id": 22,
      "label": "Colleges Or Universities",
      "key": "colleges_or_universities",
      "tracks": "education_learning"
    },
    {
      "id": 23,
      "label": "Elementary Or Secondary Schools",
      "key": "elementary_or_secondary_schools",
      "tracks": "education_learning"
    },
    {
      "id": 24,
      "label": "Educational Services",
      "key": "other_educational_services",
      "tracks": "education_learning"
    },
    {
      "id": 25,
      "label": "Vocational Schools And Trade Schools",
      "key": "vocational_schools_and_trade_schools",
      "tracks": "education_learning"
    },
    {
      "id": 26,
      "label": "Amusement Parks, Carnivals, Or Circuses",
      "key": "amusement_parks_carnivals_or_circuses",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 27,
      "label": "Betting Or Fantasy Sports",
      "key": "betting_or_fantasy_sports",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 28,
      "label": "Event Ticketing",
      "key": "event_ticketing",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 29,
      "label": "Fortune Tellers",
      "key": "fortune_tellers",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 30,
      "label": "Lotteries",
      "key": "lotteries",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 31,
      "label": "Movie Theaters",
      "key": "movie_theaters",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 32,
      "label": "Musicians, Bands, Or Orchestras",
      "key": "musicians_bands_or_orchestras",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 33,
      "label": "Online Gambling",
      "key": "online_gambling",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 34,
      "label": "Other Entertainment And Recreation",
      "key": "other_entertainment_and_recreation",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 35,
      "label": "Recreational Camps",
      "key": "recreational_camps",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 36,
      "label": "Sports Forecasting Or Prediction Services",
      "key": "sports_forecasting_or_prediction_services",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 37,
      "label": "Tourist Attractions",
      "key": "tourist_attractions",
      "tracks": "entertainment_and_recreation"
    },
    {
      "id": 38,
      "label": "Check Cashing",
      "key": "check_cashing",
      "tracks": "financial_services"
    },
    {
      "id": 39,
      "label": "Collections Agencies",
      "key": "collections_agencies",
      "tracks": "financial_services"
    },
    {
      "id": 40,
      "label": "Cryptocurrencies",
      "key": "cryptocurrencies",
      "tracks": "financial_services"
    },
    {
      "id": 41,
      "label": "Currency Exchanges",
      "key": "currency_exchanges",
      "tracks": "financial_services"
    },
    {
      "id": 42,
      "label": "Digital Wallets",
      "key": "digital_wallets",
      "tracks": "financial_services"
    },
    {
      "id": 43,
      "label": "Financial Information And Research",
      "key": "financial_information_and_research",
      "tracks": "financial_services"
    },
    {
      "id": 44,
      "label": "Insurance",
      "key": "insurance",
      "tracks": "financial_services"
    },
    {
      "id": 45,
      "label": "Investment Services",
      "key": "investment_services",
      "tracks": "financial_services"
    },
    {
      "id": 46,
      "label": "Loans Or Lending",
      "key": "loans_or_lending",
      "tracks": "financial_services"
    },
    {
      "id": 47,
      "label": "Money Orders",
      "key": "money_orders",
      "tracks": "financial_services"
    },
    {
      "id": 48,
      "label": "Money Services Or Transmission",
      "key": "money_services_or_transmission",
      "tracks": "financial_services"
    },
    {
      "id": 49,
      "label": "Other Financial Institutions",
      "key": "other_financial_institutions",
      "tracks": "financial_services"
    },
    {
      "id": 50,
      "label": "Personal Fundraising Or Crowdfunding",
      "key": "personal_fundraising_or_crowdfunding",
      "tracks": "financial_services"
    },
    {
      "id": 51,
      "label": "Security Brokers Or Dealers",
      "key": "security_brokers_or_dealers",
      "tracks": "financial_services"
    },
    {
      "id": 52,
      "label": "Virtual Currencies",
      "key": "virtual_currencies",
      "tracks": "financial_services"
    },
    {
      "id": 53,
      "label": "Wire Transfers",
      "key": "wire_transfers",
      "tracks": "financial_services"
    },
    {
      "id": 54,
      "label": "Bars And Nightclubs",
      "key": "bars_and_nightclubs",
      "tracks": "food_drink"
    },
    {
      "id": 55,
      "label": "Caterers",
      "key": "caterers",
      "tracks": "food_drink"
    },
    {
      "id": 56,
      "label": "Fast Food Restaurants",
      "key": "fast_food_restaurants",
      "tracks": "food_drink"
    },
    {
      "id": 57,
      "label": "Grocery Stores",
      "key": "grocery_stores",
      "tracks": "food_drink"
    },
    {
      "id": 58,
      "label": "Other Food And Dining",
      "key": "other_food_and_dining",
      "tracks": "food_drink"
    },
    {
      "id": 59,
      "label": "Restaurants And Nightlife",
      "key": "restaurants_and_nightlife",
      "tracks": "food_drink"
    },
    {
      "id": 60,
      "label": "Assisted Living",
      "key": "assisted_living",
      "tracks": "medical_services"
    },
    {
      "id": 61,
      "label": "Chiropractors",
      "key": "chiropractors",
      "tracks": "medical_services"
    },
    {
      "id": 62,
      "label": "Dentists And Orthodontists",
      "key": "dentists_and_orthodontists",
      "tracks": "medical_services"
    },
    {
      "id": 63,
      "label": "Doctors And Physicians",
      "key": "doctors_and_physicians",
      "tracks": "medical_services"
    },
    {
      "id": 64,
      "label": "Health And Wellness Coaching",
      "key": "health_and_wellness_coaching",
      "tracks": "medical_services"
    },
    {
      "id": 65,
      "label": "Hospitals",
      "key": "hospitals",
      "tracks": "medical_services"
    },
    {
      "id": 66,
      "label": "Medical Devices",
      "key": "medical_devices",
      "tracks": "medical_services"
    },
    {
      "id": 67,
      "label": "Medical Laboratories",
      "key": "medical_laboratories",
      "tracks": "medical_services"
    },
    {
      "id": 68,
      "label": "Medical Organizations",
      "key": "medical_organizations",
      "tracks": "medical_services"
    },
    {
      "id": 69,
      "label": "Mental Health Services",
      "key": "mental_health_services",
      "tracks": "medical_services"
    },
    {
      "id": 70,
      "label": "Nursing Or Personal Care Facilities",
      "key": "nursing_or_personal_care_facilities",
      "tracks": "medical_services"
    },
    {
      "id": 71,
      "label": "Opticians And Eyeglasses",
      "key": "opticians_and_eyeglasses",
      "tracks": "medical_services"
    },
    {
      "id": 72,
      "label": "Optometrists and Ophthalmologists",
      "key": "optometrists_and_ophthalmologists",
      "tracks": "medical_services"
    },
    {
      "id": 73,
      "label": "Osteopaths",
      "key": "osteopaths",
      "tracks": "medical_services"
    },
    {
      "id": 74,
      "label": "Other Medical Services",
      "key": "other_medical_services",
      "tracks": "medical_services"
    },
    {
      "id": 76,
      "label": "Podiatrists and Chiropodists",
      "key": "podiatrists_and_chiropodists",
      "tracks": "medical_services"
    },
    {
      "id": 77,
      "label": "Telemedicine And Telehealth",
      "key": "telemedicine_and_telehealth",
      "tracks": "medical_services"
    },
    {
      "id": 78,
      "label": "Veterinary Services",
      "key": "veterinary_services",
      "tracks": "medical_services"
    },
    {
      "id": 79,
      "label": "Charities Or Social Service Organizations",
      "key": "charities_or_social_service_organizations",
      "tracks": "membership_organizations"
    },
    {
      "id": 80,
      "label": "Civic, Fraternal, Or Social Associations",
      "key": "civic_fraternal_or_social_associations",
      "tracks": "membership_organizations"
    },
    {
      "id": 81,
      "label": "Country Clubs",
      "key": "country_clubs",
      "tracks": "membership_organizations"
    },
    {
      "id": 82,
      "label": "Other Membership Organizations",
      "key": "other_membership_organizations",
      "tracks": "membership_organizations"
    },
    {
      "id": 83,
      "label": "Political Organizations",
      "key": "political_organizations",
      "tracks": "membership_organizations"
    },
    {
      "id": 84,
      "label": "Religious Organizations",
      "key": "religious_organizations",
      "tracks": "membership_organizations"
    },
    {
      "id": 85,
      "label": "Counseling Services",
      "key": "counseling_services",
      "tracks": "personal_services"
    },
    {
      "id": 86,
      "label": "Dating Services",
      "key": "dating_services",
      "tracks": "personal_services"
    },
    {
      "id": 87,
      "label": "Funeral Services",
      "key": "funeral_services",
      "tracks": "personal_services"
    },
    {
      "id": 88,
      "label": "Health And Beauty Spas",
      "key": "health_and_beauty_spas",
      "tracks": "personal_services"
    },
    {
      "id": 90,
      "label": "Landscaping Services",
      "key": "landscaping_services",
      "tracks": "personal_services"
    },
    {
      "id": 91,
      "label": "Laundry Or Cleaning Services",
      "key": "laundry_or_cleaning_services",
      "tracks": "personal_services"
    },
    {
      "id": 92,
      "label": "Massage Parlors",
      "key": "massage_parlors",
      "tracks": "personal_services"
    },
    {
      "id": 93,
      "label": "Other Personal Services",
      "key": "other_personal_services",
      "tracks": "personal_services"
    },
    {
      "id": 94,
      "label": "Photography Studios",
      "key": "photography_studios",
      "tracks": "personal_services"
    },
    {
      "id": 95,
      "label": "Salons Or Barbers",
      "key": "salons_or_barbers",
      "tracks": "personal_services"
    },
    {
      "id": 96,
      "label": "Accounting, Auditing, Or Tax Prep",
      "key": "accounting_auditing_or_tax_prep",
      "tracks": "professional_services"
    },
    {
      "id": 97,
      "label": "Attorneys And Lawyers",
      "key": "attorneys_and_lawyers",
      "tracks": "professional_services"
    },
    {
      "id": 98,
      "label": "Auto Services",
      "key": "auto_services",
      "tracks": "professional_services"
    },
    {
      "id": 99,
      "label": "Bail Bonds",
      "key": "bail_bonds",
      "tracks": "professional_services"
    },
    {
      "id": 100,
      "label": "Bankruptcy Services",
      "key": "bankruptcy_services",
      "tracks": "professional_services"
    },
    {
      "id": 101,
      "label": "Car Rentals",
      "key": "car_rentals",
      "tracks": "professional_services"
    },
    {
      "id": 102,
      "label": "Car Sales",
      "key": "car_sales",
      "tracks": "professional_services"
    },
    {
      "id": 103,
      "label": "Computer Repair",
      "key": "computer_repair",
      "tracks": "professional_services"
    },
    {
      "id": 104,
      "label": "Consulting",
      "key": "consulting",
      "tracks": "professional_services"
    },
    {
      "id": 105,
      "label": "Credit Counseling Or Credit Repair",
      "key": "credit_counseling_or_credit_repair",
      "tracks": "professional_services"
    },
    {
      "id": 106,
      "label": "Debt Reduction Services",
      "key": "debt_reduction_services",
      "tracks": "professional_services"
    },
    {
      "id": 107,
      "label": "Digital Marketing",
      "key": "digital_marketing",
      "tracks": "professional_services"
    },
    {
      "id": 108,
      "label": "Employment Agencies",
      "key": "employment_agencies",
      "tracks": "professional_services"
    },
    {
      "id": 109,
      "label": "Government Services",
      "key": "government_services",
      "tracks": "professional_services"
    },
    {
      "id": 110,
      "label": "Lead Generation",
      "key": "lead_generation",
      "tracks": "professional_services"
    },
    {
      "id": 111,
      "label": "Mortgage Consulting Services",
      "key": "mortgage_consulting_services",
      "tracks": "professional_services"
    },
    {
      "id": 112,
      "label": "Other Business Services",
      "key": "other_business_services",
      "tracks": "professional_services"
    },
    {
      "id": 113,
      "label": "Other Marketing Services",
      "key": "other_marketing_services",
      "tracks": "professional_services"
    },
    {
      "id": 114,
      "label": "Printing and Publishing",
      "key": "printing_and_publishing",
      "tracks": "professional_services"
    },
    {
      "id": 115,
      "label": "Protective Or Security Services",
      "key": "protective_or_security_services",
      "tracks": "professional_services"
    },
    {
      "id": 117,
      "label": "Telemarketing",
      "key": "telemarketing",
      "tracks": "professional_services"
    },
    {
      "id": 118,
      "label": "Testing Laboratories",
      "key": "testing_laboratories",
      "tracks": "professional_services"
    },
    {
      "id": 119,
      "label": "Utilities",
      "key": "utilities",
      "tracks": "professional_services"
    },
    {
      "id": 120,
      "label": "Warranty Services",
      "key": "warranty_services",
      "tracks": "professional_services"
    },
    {
      "id": 121,
      "label": "Accessories For Tobacco And Marijuana",
      "key": "accessories_for_tobacco_and_marijuana",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 122,
      "label": "Adult Content Or Services",
      "key": "adult_content_or_services",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 123,
      "label": "Alcohol",
      "key": "alcohol",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 124,
      "label": "Marijuana Dispensaries",
      "key": "marijuana_dispensaries",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 125,
      "label": "Marijuana-related Products",
      "key": "marijuana_related_products",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 126,
      "label": "Pharmacies Or Pharmaceuticals",
      "key": "pharmacies_or_pharmaceuticals",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 127,
      "label": "Supplements Or Nutraceuticals",
      "key": "supplements_or_nutraceuticals",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 128,
      "label": "Tobacco Or Cigars",
      "key": "tobacco_or_cigars",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 129,
      "label": "Vapes, E-cigarettes, E-juice Or Related Products",
      "key": "vapes_e_cigarettes_e_juice_or_related_products",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 130,
      "label": "Weapons Or Munitions",
      "key": "weapons_or_munitions",
      "tracks": "regulated_and_age_restricted_products"
    },
    {
      "id": 131,
      "label": "Accessories",
      "key": "accessories",
      "tracks": "retail"
    },
    {
      "id": 132,
      "label": "Antiques",
      "key": "antiques",
      "tracks": "retail"
    },
    {
      "id": 133,
      "label": "Auto Parts And Accessories",
      "key": "auto_parts_and_accessories",
      "tracks": "retail"
    },
    {
      "id": 134,
      "label": "Beauty Products",
      "key": "beauty_products",
      "tracks": "retail"
    },
    {
      "id": 135,
      "label": "Clothing And Accessories",
      "key": "clothing_and_accessories",
      "tracks": "retail"
    },
    {
      "id": 136,
      "label": "Convenience Stores",
      "key": "convenience_stores",
      "tracks": "retail"
    },
    {
      "id": 137,
      "label": "Designer Products",
      "key": "designer_products",
      "tracks": "retail"
    },
    {
      "id": 138,
      "label": "Flowers",
      "key": "flowers",
      "tracks": "retail"
    },
    {
      "id": 139,
      "label": "Hardware Stores",
      "key": "hardware_stores",
      "tracks": "retail"
    },
    {
      "id": 140,
      "label": "Home Electronics",
      "key": "home_electronics",
      "tracks": "retail"
    },
    {
      "id": 141,
      "label": "Home Goods And Furniture",
      "key": "home_goods_and_furniture",
      "tracks": "retail"
    },
    {
      "id": 142,
      "label": "Other Merchandise",
      "key": "other_merchandise",
      "tracks": "retail"
    },
    {
      "id": 143,
      "label": "Shoes",
      "key": "shoes",
      "tracks": "retail"
    },
    {
      "id": 144,
      "label": "Software",
      "key": "software",
      "tracks": "retail"
    },
    {
      "id": 145,
      "label": "Airlines And Air Carriers",
      "key": "airlines_and_air_carriers",
      "tracks": "transportation"
    },
    {
      "id": 146,
      "label": "Commuter Transportation",
      "key": "commuter_transportation",
      "tracks": "transportation"
    },
    {
      "id": 147,
      "label": "Courier Services",
      "key": "courier_services",
      "tracks": "transportation"
    },
    {
      "id": 148,
      "label": "Cruise Lines",
      "key": "cruise_lines",
      "tracks": "transportation"
    },
    {
      "id": 149,
      "label": "Freight Forwarders",
      "key": "freight_forwarders",
      "tracks": "transportation"
    },
    {
      "id": 150,
      "label": "Other Transportation Services",
      "key": "other_transportation_services",
      "tracks": "transportation"
    },
    {
      "id": 151,
      "label": "Parking Lots",
      "key": "parking_lots",
      "tracks": "transportation"
    },
    {
      "id": 152,
      "label": "Ridesharing",
      "key": "ridesharing",
      "tracks": "transportation"
    },
    {
      "id": 153,
      "label": "Shipping Or Forwarding",
      "key": "shipping_or_forwarding",
      "tracks": "transportation"
    },
    {
      "id": 154,
      "label": "Taxis And Limos",
      "key": "taxis_and_limos",
      "tracks": "transportation"
    },
    {
      "id": 155,
      "label": "Travel Agencies",
      "key": "travel_agencies",
      "tracks": "transportation"
    },
    {
      "id": 156,
      "label": "Hotels, Inns, Or Motels",
      "key": "hotels_inns_or_motels",
      "tracks": "travel_leisure"
    },
    {
      "id": 157,
      "label": "Other Travel And Lodging",
      "key": "other_travel_leisure",
      "tracks": "travel_leisure"
    },
    {
      "id": 158,
      "label": "Property Rentals",
      "key": "property_rentals",
      "tracks": "travel_leisure"
    },
    {
      "id": 159,
      "label": "Timeshares",
      "key": "timeshares",
      "tracks": "travel_leisure"
    },
    {
      "id": 160,
      "label": "Trailer Parks and Campgrounds",
      "key": "trailer_parks_and_campgrounds",
      "tracks": "travel_leisure"
    }
  ]
}"""
