################ Setup ###########################
## Load libraries 
library(tidyverse)
library(readxl)
library(foreign)
library(scales)
library(ggpubr)
library(patchwork)
library(data.table)
library(ggplot2)
library(dplyr)
library(stringr)
library(tidyr)
library(purrr)

## Set seed for reproducibility 
set.seed(420) 

# Standard map with input and output: to change
setwd("C:\\Users\\gebruiker\\Documents\\HTM_case")

# Read data 
data_dayshifts <- read_excel("Data_cleaned.xlsx", sheet = "Dagroutes", col_names = FALSE, col_types = "text") %>%
  mutate(across(everything(), ~na_if(str_squish(.x), "")))
data_nightshifts <- read_excel("Data_cleaned.xlsx", sheet = "Nachtroutes", col_names = FALSE, col_types = "text") %>%
  mutate(across(everything(), ~na_if(str_squish(.x), "")))
data_stops <- read_excel("Data_cleaned.xlsx", sheet = "Halteinfo")


##### CLEANING DATA ON ROUTES / SHIFTS ######

# Different dfs for different shifts / routes 
data_dayshifts <- data_dayshifts %>% mutate(row_text = apply(., 1, \(r) paste(r, collapse = " ")),
                                   block = cumsum(str_detect(row_text, "^PO\\s*:"))) %>%
  filter(block > 0)

blocks_day <- split(data_dayshifts, data_dayshifts$block)

# Make different data frames for every dayshift 
routes_days <- map_dfr(blocks_day, \(b) {
  # route name from the row containing "Route:"
  route <- b %>%
    mutate(rt = str_match(row_text, "Route\\s*:\\s*([^\\s]+)")[,2]) %>%
    pull(rt) %>%
    na.omit() %>%
    dplyr::first()
  
  # header row is where "Volgorde" appears (and usually "Activum" too)
  header_i <- which(apply(b, 1, \(r) any(r == "Volgorde", na.rm = TRUE)))[1]
  if (is.na(header_i)) return(tibble())  # safety
  
  header <- as.character(b[header_i, ])
  
  idx_volgorde <- which(header == "Volgorde")[1]
  idx_activum  <- which(header == "Activum")[1]
  idx_naam <- which(header == "Beschrijving")[1]
  idx_dovaCode <- which(header == "Dova")[1]
  if (length(idx_volgorde) == 0 || length(idx_activum) == 0 || length(idx_dovaCode) == 0) return(tibble())
  
  # rows after header = stops
  stops <- b[(header_i + 1):nrow(b), , drop = FALSE]
  
  tibble(
    Route = route,
    ID_MAXIMO = stops[[idx_activum]],
    Order = suppressWarnings(as.integer(stops[[idx_volgorde]])),
    Name = stops[[idx_naam]],
    Dova_code = str_extract(as.character(stops[[idx_dovaCode]]), "NL:Q:\\d+"),
    Night_shift = 0
  ) %>%
    filter(!is.na(Route), !is.na(ID_MAXIMO), !is.na(Order)) %>%
    arrange(Route, Order)
})

## Same process for the night shifts 
# Different dfs for different shifts / routes 
data_nightshifts <- data_nightshifts %>% mutate(row_text = apply(., 1, \(r) paste(r, collapse = " ")),
                                            block = cumsum(str_detect(row_text, "^PO\\s*:"))) %>%
  filter(block > 0)

blocks_night <- split(data_nightshifts, data_nightshifts$block)

# Make different data frames for every nightshift 
routes_nights <- map_dfr(blocks_night, \(b) {
  # route name from the row containing "Route:"
  route <- b %>%
    mutate(rt = str_match(row_text, "Route\\s*:\\s*([^\\s]+)")[,2]) %>%
    pull(rt) %>%
    na.omit() %>%
    dplyr::first()
  
  # header row is where "Volgorde" appears
  header_i <- which(apply(b, 1, \(r) any(r == "Volgorde", na.rm = TRUE)))[1]
  if (is.na(header_i)) return(tibble())  # safety
  
  header <- as.character(b[header_i, ])
  
  idx_volgorde <- which(header == "Volgorde")[1]
  idx_activum  <- which(header == "Activum")[1]
  idx_naam <- which(header == "Beschrijving")[1]
  idx_dovaCode <- which(header == "Dova")[1]
  if (length(idx_volgorde) == 0 || length(idx_activum) == 0 || length(idx_dovaCode) == 0) return(tibble())
  
  # rows after header = stops
  stops <- b[(header_i + 1):nrow(b), , drop = FALSE]
  
  tibble(
    Route = route,
    ID_MAXIMO = stops[[idx_activum]],
    Order = suppressWarnings(as.integer(stops[[idx_volgorde]])),
    Name = stops[[idx_naam]],
    Dova_code = str_extract(as.character(stops[[idx_dovaCode]]), "NL:Q:\\d+"),
    Night_shift = 1
  ) %>%
    filter(!is.na(Route), !is.na(ID_MAXIMO), !is.na(Order)) %>%
    arrange(Route, Order)
})


## Combine all info on routes 
routes_all <- bind_rows(routes_days, routes_nights) %>%
  arrange(Night_shift, Route, Order)

## Add information from Halteinfo 
with_Halteinfo <- routes_all %>%
  left_join(
    data_stops %>% select(ID_MAXIMO, longitude, latitude),
    by = "ID_MAXIMO"
  )

data_final <- with_Halteinfo %>%
  mutate(ID = row_number())

# Add row for depot
data_withDepot <- add_row(data_final, Route = NA, ID_MAXIMO = "Depot", Order = 0,
                      Dova_code = NA, Night_shift = 100, longitude = 4.348949113895611, latitude = 52.06538548231154, ID = 0, .before = 1)

## Write as csv file 
write.csv(data_withDepot, "HTM_fullData.csv", row.names=FALSE, quote = FALSE)

## Find the stops with no longitude and latitude yet 
data_final %>%
  summarise(n_unique_ID_MAXIMO = n_distinct(ID_MAXIMO))

## Need info on the following
noInfoStops <- data_final %>%
  filter(is.na(longitude)) %>%                 
  distinct(ID_MAXIMO, .keep_all = TRUE) %>%   
  select(ID_MAXIMO, Dova_code, longitude, latitude)

write.csv(noInfoStops, "HTM_noInfoStops.csv", row.names=FALSE, quote = FALSE)

## Once info on latitude and longitude of all stops is obtained, add them to data_withDepot
data_noInfoStops <- read_excel("HTM_noInfoStops_InfoCopied.xlsx")

data_noInfoStops <- data_noInfoStops %>%
  mutate(
    longitude = as.numeric(gsub("\\.", "", longitude)) / 1e6,
    latitude  = as.numeric(gsub("\\.", "", latitude)) / 1e6
  )

data_filled <- data_withDepot %>%
  left_join(
    data_noInfoStops %>%
      select(ID_MAXIMO, longitude_new = longitude, latitude_new = latitude) %>%
      distinct(ID_MAXIMO, .keep_all = TRUE),
    by = "ID_MAXIMO"
  ) %>%
  mutate(
    longitude = coalesce(longitude, longitude_new),
    latitude  = coalesce(latitude,  latitude_new)
  ) %>%
  select(-longitude_new, -latitude_new)

## Check if there are still stops missing 
missingStops <- data_filled %>%
  filter(is.na(longitude))

## Save Complete Data 
write.csv(data_filled, "HTM_CompleteData.csv", row.names=FALSE, quote = FALSE)


##### INSPECTING DATA #####
## Plot the stops and depot 
scatterPlot <- ggplot(data_filled, aes(x = longitude, y = latitude)) +
  geom_point(color = "hotpink", shape = 19, na.rm = TRUE) +
  geom_point(
    data = subset(data_filled, ID_MAXIMO == "Depot"),
    aes(color = "Depot"),              
    shape = 8, size = 4, na.rm = TRUE
  ) +
  scale_color_manual(values = c("Depot" = "darkmagenta")) +
  labs(x = "Longitude", y = "Latitude", color = NULL) +
  coord_cartesian(
    xlim = range(data_final$longitude, na.rm = TRUE),
    ylim = range(data_final$latitude,  na.rm = TRUE)
  ) +
  theme_minimal()

scatterPlot

## Save plot 
ggsave("Figure_allStops.png", scatterPlot, width = 12, height = 8, units = "in", dpi = 300)

## Save this info as text file for coding purposes 
columns_for_text <- c("ID", "ID_MAXIMO", "latitude", "longitude", "Night_shift")
data_filled_text <- data_filled[, columns_for_text]

write.table(data_filled_text,"data_complete.txt",sep="\t",row.names=FALSE, quote = FALSE)

##### CORRELATION ##### 
columns_for_cor <- c("ID_MAXIMO",
                     "CODE_CATEGORIE", "CODE_OVERSTAP",	"CODE_RECREATIE",	"CODE_WIJK",	
                     "CODE_SEIZOEN", "CRITERIUM_SEIZOEN")
correlation_stops <- data_stops[, columns_for_cor] %>%
  mutate(
    SUMMER_HOLIDAYS = if_else(
      !is.na(CRITERIUM_SEIZOEN) & CRITERIUM_SEIZOEN == "Spreiding instappers/maand",
      1L, 0L
    )
  ) %>%
  select(-CRITERIUM_SEIZOEN)

## Spearman for correlation 
cor_codecat <- correlation_stops %>%
  select(-ID_MAXIMO) %>%
  cor(method = "spearman", use = "pairwise.complete.obs") %>%
  .["CODE_CATEGORIE", ] %>%
  .[names(.) != "CODE_CATEGORIE"]

cor_codecat

## Plot 
plot_df <- correlation_stops %>%
  select(CODE_CATEGORIE, CODE_WIJK, CODE_RECREATIE, CODE_OVERSTAP, CODE_SEIZOEN) %>%
  mutate(
    CODE_CATEGORIE = factor(CODE_CATEGORIE, levels = c(1, 2, 3),
                            labels = c("1", "2", "3"))
  ) %>%
  pivot_longer(
    cols = c(CODE_WIJK, CODE_RECREATIE, CODE_OVERSTAP, CODE_SEIZOEN),
    names_to = "Feature",
    values_to = "Value"
  ) %>%
  mutate(
    Feature = recode(
      Feature,
      CODE_WIJK = "Wijk",
      CODE_RECREATIE = "Recreatie",
      CODE_OVERSTAP = "Overstap", 
      CODE_SEIZOEN = "Seizoen"
    )
  ) %>%
  group_by(CODE_CATEGORIE, Feature) %>%
  summarise(Share_1 = mean(Value == 1, na.rm = TRUE), .groups = "drop")

code_plot <- ggplot(plot_df, aes(x = CODE_CATEGORIE, y = Share_1, fill = CODE_CATEGORIE)) +
  geom_col(width = 0.7) +
  facet_wrap(~ Feature, nrow = 1) +
  scale_y_continuous(labels = percent_format(accuracy = 1), limits = c(0, 1)) +
  labs(
    x = "Category",
    y = "Feature = 1"
  ) +
  scale_fill_manual(
    values = c(
      "1" = "#08306B", 
      "2" = "#4292C6",  
      "3" = "#DEEBF7"   
    )
  ) +
  theme_minimal(base_size = 13) +
  theme(
    panel.grid.minor = element_blank(),
    strip.text = element_text(face = "bold", size = 12),
    plot.title = element_text(face = "bold"),
    axis.title.x = element_text(margin = margin(t = 10)),
    axis.title.y = element_text(margin = margin(r = 10))
  ) + guides(fill = "none")


## Save plot 
ggsave("Figure_codePlot.png", code_plot, width = 12, height = 6, units = "in", dpi = 300)

##### COLLAPSE THE STOPS #####
data_collapsed <- data_filled %>%
  mutate(block = cumsum(Order == 1)) %>%
  
  # Find when multiples of the same ID follow one another 
  group_by(block) %>%
  mutate(run_id = cumsum(ID_MAXIMO != lag(ID_MAXIMO, default = first(ID_MAXIMO)))) %>%
  ungroup() %>%
  
  # Collapse those 
  group_by(block, run_id, ID_MAXIMO) %>%
  summarise(
    across(everything(), ~ .x[1]),  
    n_in_run = n(),
    .groups = "drop"
  ) %>%
  
  # Get service times 
  mutate(Service_time = 10 + 10 * n_in_run) %>%
  mutate(Service_time = if_else(ID_MAXIMO == "Depot", 0, Service_time)) %>%
  
  # Fix the order
  group_by(block) %>%
  mutate(Order = row_number()) %>%
  ungroup() %>%
  
  # New ID 
  mutate(ID = row_number() - 1) %>%
  
  select(-run_id, -n_in_run, -block)

## Save Collapsed Data 
write.csv(data_collapsed, "HTM_CollapsedData.csv", row.names=FALSE, quote = FALSE)

## Save this info as text file for coding purposes 
columns_for_text <- c("ID", "ID_MAXIMO", "latitude", "longitude", "Night_shift", "Service_time")

data_collapsed_text <- data_collapsed[, columns_for_text]

write.table(data_collapsed_text,"data_collapsed.txt",sep="\t",row.names=FALSE, quote = FALSE)


##### PLOTS DAY VS NIGHT  #####
## Separate the night stops and day stops 
data_nightStops <- subset(data_collapsed, Night_shift == 1)

data_dayStops <- subset(data_collapsed, Night_shift == 0)

## Plot the stops and depot 
dayNightPlot <- ggplot(data_dayStops, aes(x = longitude, y = latitude)) +
  geom_point(color = "hotpink", shape = 19, na.rm = TRUE) + 
  geom_point(
    data = data_nightStops,
    aes(x = longitude, y = latitude),
    color = "lightblue",
    shape = 19,
    na.rm = TRUE
  ) +
  geom_point(
    data = subset(data_filled, ID_MAXIMO == "Depot"),
    aes(color = "Depot"),              
    shape = 8, size = 4, na.rm = TRUE
  ) +
  scale_color_manual(values = c("Depot" = "darkmagenta")) +
  labs(x = "Longitude", y = "Latitude", color = NULL) +
  coord_cartesian(
    xlim = range(data_final$longitude, na.rm = TRUE),
    ylim = range(data_final$latitude,  na.rm = TRUE)
  ) +
  theme_minimal()

dayNightPlot

## Save plot 
ggsave("Figure_dayNightPlot.png", dayNightPlot, width = 12, height = 8, units = "in", dpi = 300)

## Plot the initial routes 
depot <- data_filled %>%
  filter(ID_MAXIMO == "Depot") %>%
  summarise(
    longitude = first(longitude),
    latitude  = first(latitude)
  )
### Nightroutes ###

# Add depot to beginning and end of each route 
routes_with_depot <- data_nightStops %>%
  group_by(Route) %>%
  group_modify(~{
    df <- .x
    start_depot <- df[1, ]
    start_depot$ID_MAXIMO <- "Depot"
    start_depot$Order <- min(df$Order, na.rm = TRUE) - 1
    start_depot$longitude <- depot$longitude
    start_depot$latitude  <- depot$latitude
    
    end_depot <- df[1, ]
    end_depot$ID_MAXIMO <- "Depot"
    end_depot$Order <- max(df$Order, na.rm = TRUE) + 1
    end_depot$longitude <- depot$longitude
    end_depot$latitude  <- depot$latitude
    
    bind_rows(start_depot, df, end_depot) %>% arrange(Order)
  }) %>%
  ungroup()

plot_nightRoutes <- ggplot() +
  geom_point(
    data = data_nightStops %>% filter(ID_MAXIMO != "Depot"),
    aes(x = longitude, y = latitude),
    color = "black", size = 1.6, alpha = 0.7, na.rm = TRUE
  ) +
  geom_path(
    data = routes_with_depot,
    aes(x = longitude, y = latitude, group = Route, color = Route),
    linewidth = 0.7, alpha = 0.9, na.rm = TRUE
  ) +
  geom_point(
    data = subset(data_filled, ID_MAXIMO == "Depot"),
    aes(x = longitude, y = latitude),
    shape = 8, size = 4, color = "darkmagenta", na.rm = TRUE
  ) +
  labs(x = "Longitude", y = "Latitude", color = "Route") +
  coord_cartesian(
    xlim = range(data_nightStops$longitude, na.rm = TRUE),
    ylim = range(data_nightStops$latitude,  na.rm = TRUE)
  ) +
  theme_minimal(base_size = 13) + theme(legend.position = "none")

plot_nightRoutes

ggsave("Figure_initialNightShifts.png", plot_nightRoutes, width = 8, height = 6, units = "in", dpi = 300)


### Dayroutes ### 

# Add depot to beginning and end of each route 
routes_with_depot_day <- data_dayStops %>%
  group_by(Route) %>%
  group_modify(~{
    df <- .x
    start_depot <- df[1, ]
    start_depot$ID_MAXIMO <- "Depot"
    start_depot$Order <- min(df$Order, na.rm = TRUE) - 1
    start_depot$longitude <- depot$longitude
    start_depot$latitude  <- depot$latitude
    
    end_depot <- df[1, ]
    end_depot$ID_MAXIMO <- "Depot"
    end_depot$Order <- max(df$Order, na.rm = TRUE) + 1
    end_depot$longitude <- depot$longitude
    end_depot$latitude  <- depot$latitude
    
    bind_rows(start_depot, df, end_depot) %>% arrange(Order)
  }) %>%
  ungroup()

plot_dayRoutes <- ggplot() +
  geom_point(
    data = data_dayStops %>% filter(ID_MAXIMO != "Depot"),
    aes(x = longitude, y = latitude),
    color = "black", size = 1.6, alpha = 0.7, na.rm = TRUE
  ) +
  geom_path(
    data = routes_with_depot_day,
    aes(x = longitude, y = latitude, group = Route, color = Route),
    linewidth = 0.7, alpha = 0.9, na.rm = TRUE
  ) +
  geom_point(
    data = subset(data_filled, ID_MAXIMO == "Depot"),
    aes(x = longitude, y = latitude),
    shape = 8, size = 4, color = "darkmagenta", na.rm = TRUE
  ) +
  labs(x = "Longitude", y = "Latitude", color = "Route") +
  coord_cartesian(
    xlim = range(data_dayStops$longitude, na.rm = TRUE),
    ylim = range(data_dayStops$latitude,  na.rm = TRUE)
  ) +
  theme_minimal(base_size = 13) + theme(legend.position = "none")

plot_dayRoutes

ggsave("Figure_initialDayShifts.png", plot_dayRoutes, width = 8, height = 6, units = "in", dpi = 300)


## Combine 
data_allroutes_with_depot <- bind_rows(routes_with_depot_day, routes_with_depot) %>% mutate(ID = if_else(ID_MAXIMO == "Depot", 0, ID)) %>% select(-Name, -Dova_code, -Service_time)

write.csv(data_allroutes_with_depot, "HTM_allroutes.csv", row.names=FALSE, quote = FALSE)


##### COLLAPSE THE STOPS WITH CODE_CATEGORIE #####
data_collapsedv2 <- data_filled %>%
  left_join(
    data_stops %>% select(ID_MAXIMO, CODE_CATEGORIE),
    by = "ID_MAXIMO"
  ) %>% mutate(CODE_CATEGORIE = tidyr::replace_na(CODE_CATEGORIE, 0L)) 

data_collapsedv2 <- data_collapsedv2 %>%
  mutate(block = cumsum(Order == 1)) %>%
  
  # Find when multiples of the same ID follow one another 
  group_by(block) %>%
  mutate(run_id = cumsum(ID_MAXIMO != lag(ID_MAXIMO, default = first(ID_MAXIMO)))) %>%
  ungroup() %>%
  
  # Collapse those 
  group_by(block, run_id, ID_MAXIMO) %>%
  summarise(
    across(everything(), ~ .x[1]),  
    n_in_run = n(),
    .groups = "drop"
  ) %>%
  
  # Get service times 
  mutate(Service_time = 10 + 10 * n_in_run) %>%
  mutate(Service_time = if_else(CODE_CATEGORIE == 3, 10 + 5 * n_in_run, Service_time)) %>%
  mutate(Service_time = if_else(CODE_CATEGORIE == 2, 10 + 8 * n_in_run, Service_time)) %>%
  mutate(Service_time = if_else(ID_MAXIMO == "Depot", 0, Service_time)) %>%
  
  # Fix the order
  group_by(block) %>%
  mutate(Order = row_number()) %>%
  ungroup() %>%
  
  # New ID 
  mutate(ID = row_number() - 1) %>%
  
  select(-run_id, -n_in_run, -block, -CODE_CATEGORIE)

## Save Collapsed Data 
write.csv(data_collapsedv2, "HTM_CollapsedDatav2.csv", row.names=FALSE, quote = FALSE)

## Save this info as text file for coding purposes 
data_collapsedv2_text <- data_collapsedv2[, columns_for_text]

write.table(data_collapsedv2_text,"data_collapsed_v2.txt",sep="\t",row.names=FALSE, quote = FALSE)

## Plot of distribution of cleaning times 
plotCleaningTimes <- ggplot(
  data = filter(data_collapsedv2, ID_MAXIMO != "Depot"),
  aes(x = longitude, y = latitude, color = Service_time)
) +
  geom_point(shape = 19, size = 2, alpha = 0.9, na.rm = TRUE) +
  geom_point(
    data = subset(data_collapsed, ID_MAXIMO == "Depot"),
    inherit.aes = FALSE,
    aes(x = longitude, y = latitude),
    shape = 8, size = 4, color = "darkmagenta", na.rm = TRUE
  ) +
  scale_color_gradient(low = "#DEEBF7", high = "#08306B", name = "Cleaning time") +
  labs(x = "Longitude", y = "Latitude") +
  coord_cartesian(
    xlim = range(data_final$longitude, na.rm = TRUE),
    ylim = range(data_final$latitude,  na.rm = TRUE)
  ) +
  theme_minimal(base_size = 13) +
  theme(legend.position = "right")

plotCleaningTimes

## Save plot 
ggsave("Figure_plotCleaningTimes.png", plotCleaningTimes, width = 12, height = 8, units = "in", dpi = 300)

##### K-MEANS CLUSTERING #####
## Prepare data for k-means by removing the depot 
kmeans_Stops <- data_collapsedv2 %>%
  filter(ID_MAXIMO != "Depot") %>%          
  filter(!is.na(longitude), !is.na(latitude)) 

## K-means

## Elbow plot 
wss <- sapply(1:10, function(k) {
  kmeans(kmeans_Stops[, c("longitude", "latitude")], k, nstart = 25)$tot.withinss
})

plot(1:10, wss, type = "b",
     xlab = "Number of clusters (k)",
     ylab = "Total within-cluster sum of squares")

## Choose final k 
k <- 3
km <- kmeans(
  kmeans_Stops[, c("longitude", "latitude")],
  centers = k,
  nstart = 25
)

kmeans_Stops <- kmeans_Stops %>%
  mutate(cluster = factor(km$cluster))

clusters <- ggplot(kmeans_Stops, aes(longitude, latitude, color = cluster)) +
  geom_point(size = 2) +
  geom_point(
    data = subset(data_filled, ID_MAXIMO == "Depot"),
    aes(longitude, latitude),
    inherit.aes = FALSE,
    shape = 8, size = 4, color = "darkmagenta"
  ) +
  labs(x = "Longitude", y = "Latitude") +
  theme_minimal() +
  theme(legend.position = "none")

clusters


## K-means on night and day shifts 

## Choose final k 
k <- 25
km <- kmeans(
  data_nightStops[, c("longitude", "latitude")],
  centers = k,
  nstart = 25
)

data_nightStops <- data_nightStops %>%
  mutate(cluster = factor(km$cluster))

clusters_nightStops <- ggplot(data_nightStops, aes(longitude, latitude, color = cluster)) +
  geom_point(size = 2) +
  geom_point(
    data = subset(data_filled, ID_MAXIMO == "Depot"),
    aes(longitude, latitude),
    inherit.aes = FALSE,
    shape = 8, size = 4, color = "darkmagenta"
  ) +
  labs(x = "Longitude", y = "Latitude") +
  theme_minimal() +
  theme(legend.position = "none")

clusters_nightStops


## Save the figure 
ggsave("Figure_clusters_nightStops.png", clusters_nightStops, width = 12, height = 8, units = "in", dpi = 300)


## Do the same for the day shifts 

## Choose final k 
k <- 25
km <- kmeans(
  data_dayStops[, c("longitude", "latitude")],
  centers = k,
  nstart = 25
)

data_dayStops <- data_dayStops %>%
  mutate(cluster = factor(km$cluster + 25))

clusters_dayStops <- ggplot(data_dayStops, aes(longitude, latitude, color = cluster)) +
  geom_point(size = 2) +
  geom_point(
    data = subset(data_filled, ID_MAXIMO == "Depot"),
    aes(longitude, latitude),
    inherit.aes = FALSE,
    shape = 8, size = 4, color = "darkmagenta"
  ) +
  labs(x = "Longitude", y = "Latitude") +
  theme_minimal() +
  theme(legend.position = "none")

clusters_dayStops

ggsave("Figure_clusters_dayStops.png", clusters_dayStops, width = 12, height = 8, units = "in", dpi = 300)

data_clustered <- bind_rows(data_dayStops, data_nightStops)
  
# Add row for depot
data_clustered <- add_row(data_clustered, ID_MAXIMO = "Depot", Route = NA, Order = 0, Name = "Depot", 
                          Dova_code = NA, Night_shift = 100, longitude = 4.348949113895611, latitude = 52.06538548231154, ID = 0, Service_time = 0, .before = 1)

# Save clustered data
write.csv(data_clustered, "HTM_ClusteredData.csv", row.names=FALSE, quote = FALSE)


