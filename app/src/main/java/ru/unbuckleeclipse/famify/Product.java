package ru.unbuckleeclipse.famify;

public class Product {
    private String id;
    private String name;
    private String comment;
    private String addedBy;
    private boolean inCart;
    private long createdAt;
    // Новый флаг для состояния меню:
    private boolean menuOpen = false;

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }

    public boolean isInCart() { return inCart; }
    public void setInCart(boolean inCart) { this.inCart = inCart; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Геттер и сеттер для состояния меню:
    public boolean isMenuOpen() { return menuOpen; }
    public void setMenuOpen(boolean menuOpen) { this.menuOpen = menuOpen; }


}