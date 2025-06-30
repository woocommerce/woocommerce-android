package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCProductStore.AddProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.AddProductTagsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.DeleteProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductCategoriesPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductPasswordPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductShippingClassListPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductSkuAvailabilityPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductTagsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductShippingClassPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductCategoryResponsePayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductTagsResponsePayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteDeleteProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductCategoriesPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductListPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPasswordPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassListPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductSkuAvailabilityPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductTagsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteSearchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductImagesPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdatedProductPasswordPayload;
import org.wordpress.android.fluxc.store.WCProductStore.SearchProductsByGlobalUniqueIdPayload;
import org.wordpress.android.fluxc.store.WCProductStore.SearchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload;
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPasswordPayload;
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload;

@ActionEnum
public enum WCProductAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchProductsPayload.class)
    FETCH_PRODUCTS,
    @Action(payloadType = SearchProductsPayload.class)
    SEARCH_PRODUCTS,
    @Action(payloadType = SearchProductsByGlobalUniqueIdPayload.class)
    SEARCH_PRODUCTS_BY_GLOBAL_UNIQUE_ID,
    @Action(payloadType = FetchProductShippingClassListPayload.class)
    FETCH_PRODUCT_SHIPPING_CLASS_LIST,
    @Action(payloadType = FetchSingleProductShippingClassPayload.class)
    FETCH_SINGLE_PRODUCT_SHIPPING_CLASS,
    @Action(payloadType = UpdateProductImagesPayload.class)
    UPDATE_PRODUCT_IMAGES,
    @Action(payloadType = UpdateProductPayload.class)
    UPDATE_PRODUCT,
    @Action(payloadType = FetchProductSkuAvailabilityPayload.class)
    FETCH_PRODUCT_SKU_AVAILABILITY,
    @Action(payloadType = FetchProductPasswordPayload.class)
    FETCH_PRODUCT_PASSWORD,
    @Action(payloadType = UpdateProductPasswordPayload.class)
    UPDATE_PRODUCT_PASSWORD,
    @Action(payloadType = FetchProductCategoriesPayload.class)
    FETCH_PRODUCT_CATEGORIES,
    @Action(payloadType = FetchProductTagsPayload.class)
    FETCH_PRODUCT_TAGS,
    @Action(payloadType = AddProductTagsPayload.class)
    ADD_PRODUCT_TAGS,
    @Action(payloadType = AddProductPayload.class)
    ADD_PRODUCT,
    @Action(payloadType = DeleteProductPayload.class)
    DELETE_PRODUCT,

    // Remote responses
    @Action(payloadType = RemoteProductListPayload.class)
    FETCHED_PRODUCTS,
    @Action(payloadType = RemoteSearchProductsPayload.class)
    SEARCHED_PRODUCTS,
    @Action(payloadType = RemoteProductShippingClassListPayload.class)
    FETCHED_PRODUCT_SHIPPING_CLASS_LIST,
    @Action(payloadType = RemoteProductShippingClassPayload.class)
    FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS,
    @Action(payloadType = RemoteUpdateProductImagesPayload.class)
    UPDATED_PRODUCT_IMAGES,
    @Action(payloadType = RemoteUpdateProductPayload.class)
    UPDATED_PRODUCT,
    @Action(payloadType = RemoteProductSkuAvailabilityPayload.class)
    FETCHED_PRODUCT_SKU_AVAILABILITY,
    @Action(payloadType = RemoteProductPasswordPayload.class)
    FETCHED_PRODUCT_PASSWORD,
    @Action(payloadType = RemoteUpdatedProductPasswordPayload.class)
    UPDATED_PRODUCT_PASSWORD,
    @Action(payloadType = RemoteProductCategoriesPayload.class)
    FETCHED_PRODUCT_CATEGORIES,
    @Action(payloadType = RemoteAddProductCategoryResponsePayload.class)
    ADDED_PRODUCT_CATEGORY,
    @Action(payloadType = RemoteProductTagsPayload.class)
    FETCHED_PRODUCT_TAGS,
    @Action(payloadType = RemoteAddProductTagsResponsePayload.class)
    ADDED_PRODUCT_TAGS,
    @Action(payloadType = RemoteAddProductPayload.class)
    ADDED_PRODUCT,
    @Action(payloadType = RemoteDeleteProductPayload.class)
    DELETED_PRODUCT
}
