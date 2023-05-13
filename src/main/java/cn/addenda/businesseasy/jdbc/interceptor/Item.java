package cn.addenda.businesseasy.jdbc.interceptor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author addenda
 * @since 2023/5/12 19:57
 */
@Setter
@Getter
@ToString
public class Item {

    private String itemKey;

    private Object itemValue;

    public Item(String itemKey, Object itemValue) {
        this.itemKey = itemKey;
        this.itemValue = itemValue;
    }

    public static class ItemBuilder {
        private String itemKey;
        private Object itemValue;

        public ItemBuilder withItemKey(String itemKey) {
            this.itemKey = itemKey;
            return this;
        }

        public ItemBuilder withItemValue(Object itemValue) {
            this.itemValue = itemValue;
            return this;
        }

        public Item build() {
            return new Item(itemKey, itemValue);
        }
    }

}
