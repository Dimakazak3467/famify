package ru.unbuckleeclipse.famify;

public class Product {
    private String id;
    private String name;
    private String comment;
    private String addedBy;
    private boolean inCart;

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getComment() { return comment; }
    public String getAddedBy() { return addedBy; }
    public boolean isInCart() { return inCart; }
    public void setInCart(boolean inCart) { this.inCart = inCart; }
}