/*****************************************************************************************************
 * 多言語化対応
 * <script type="text/javascript" src="jquery_i18n_properties.js"></script>を読み込んでおく必要がある。
 ****************************************************************************************************/

$.i18n.properties({
    name: 'Messages',
    path: 'i18n',
    mode: 'both'
});

function updateMessage() {
    for (key in $.i18n.map) {
        $(".i18n_" + key).text($.i18n.prop(key));
        $(".i18n_add_" + key).each(function(i, element) {
            $(element).text($(element).text() + $.i18n.prop(key));
        });
        $(".i18n_placeholder_" + key).attr("placeholder", $.i18n.prop(key));
        $(".i18n_title_" + key).attr("title", $.i18n.prop(key));
    }
}
