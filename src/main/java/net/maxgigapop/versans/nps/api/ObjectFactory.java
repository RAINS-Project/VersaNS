
package net.maxgigapop.versans.nps.api;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the net.maxgigapop.versans.nps.api package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Teardown_QNAME = new QName("http://maxgigapop.net/versans/nps/api/", "teardown");
    private final static QName _TeardownResponse_QNAME = new QName("http://maxgigapop.net/versans/nps/api/", "teardownResponse");
    private final static QName _Modify_QNAME = new QName("http://maxgigapop.net/versans/nps/api/", "modify");
    private final static QName _QueryResponse_QNAME = new QName("http://maxgigapop.net/versans/nps/api/", "queryResponse");
    private final static QName _ModifyResponse_QNAME = new QName("http://maxgigapop.net/versans/nps/api/", "modifyResponse");
    private final static QName _Setup_QNAME = new QName("http://maxgigapop.net/versans/nps/api/", "setup");
    private final static QName _SetupResponse_QNAME = new QName("http://maxgigapop.net/versans/nps/api/", "setupResponse");
    private final static QName _Query_QNAME = new QName("http://maxgigapop.net/versans/nps/api/", "query");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: net.maxgigapop.versans.nps.api
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link QueryContent }
     * 
     */
    public QueryContent createQueryContent() {
        return new QueryContent();
    }

    /**
     * Create an instance of {@link SetupResponseContent }
     * 
     */
    public SetupResponseContent createSetupResponseContent() {
        return new SetupResponseContent();
    }

    /**
     * Create an instance of {@link SetupContent }
     * 
     */
    public SetupContent createSetupContent() {
        return new SetupContent();
    }

    /**
     * Create an instance of {@link ModifyResponseContent }
     * 
     */
    public ModifyResponseContent createModifyResponseContent() {
        return new ModifyResponseContent();
    }

    /**
     * Create an instance of {@link QueryResponseContent }
     * 
     */
    public QueryResponseContent createQueryResponseContent() {
        return new QueryResponseContent();
    }

    /**
     * Create an instance of {@link ModifyContent }
     * 
     */
    public ModifyContent createModifyContent() {
        return new ModifyContent();
    }

    /**
     * Create an instance of {@link TeardownResponseContent }
     * 
     */
    public TeardownResponseContent createTeardownResponseContent() {
        return new TeardownResponseContent();
    }

    /**
     * Create an instance of {@link ServiceExceptionContent }
     * 
     */
    public ServiceExceptionContent createServiceExceptionContent() {
        return new ServiceExceptionContent();
    }

    /**
     * Create an instance of {@link TeardownContent }
     * 
     */
    public TeardownContent createTeardownContent() {
        return new TeardownContent();
    }

    /**
     * Create an instance of {@link RouteInfo }
     * 
     */
    public RouteInfo createRouteInfo() {
        return new RouteInfo();
    }

    /**
     * Create an instance of {@link Layer2Info }
     * 
     */
    public Layer2Info createLayer2Info() {
        return new Layer2Info();
    }

    /**
     * Create an instance of {@link VlanTag }
     * 
     */
    public VlanTag createVlanTag() {
        return new VlanTag();
    }

    /**
     * Create an instance of {@link ResourceReference }
     * 
     */
    public ResourceReference createResourceReference() {
        return new ResourceReference();
    }

    /**
     * Create an instance of {@link Layer3Info }
     * 
     */
    public Layer3Info createLayer3Info() {
        return new Layer3Info();
    }

    /**
     * Create an instance of {@link ServicePolicy }
     * 
     */
    public ServicePolicy createServicePolicy() {
        return new ServicePolicy();
    }

    /**
     * Create an instance of {@link BgpInfo }
     * 
     */
    public BgpInfo createBgpInfo() {
        return new BgpInfo();
    }

    /**
     * Create an instance of {@link ServiceContract }
     * 
     */
    public ServiceContract createServiceContract() {
        return new ServiceContract();
    }

    /**
     * Create an instance of {@link ServiceTerminationPoint }
     * 
     */
    public ServiceTerminationPoint createServiceTerminationPoint() {
        return new ServiceTerminationPoint();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TeardownContent }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://maxgigapop.net/versans/nps/api/", name = "teardown")
    public JAXBElement<TeardownContent> createTeardown(TeardownContent value) {
        return new JAXBElement<TeardownContent>(_Teardown_QNAME, TeardownContent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TeardownResponseContent }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://maxgigapop.net/versans/nps/api/", name = "teardownResponse")
    public JAXBElement<TeardownResponseContent> createTeardownResponse(TeardownResponseContent value) {
        return new JAXBElement<TeardownResponseContent>(_TeardownResponse_QNAME, TeardownResponseContent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ModifyContent }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://maxgigapop.net/versans/nps/api/", name = "modify")
    public JAXBElement<ModifyContent> createModify(ModifyContent value) {
        return new JAXBElement<ModifyContent>(_Modify_QNAME, ModifyContent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryResponseContent }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://maxgigapop.net/versans/nps/api/", name = "queryResponse")
    public JAXBElement<QueryResponseContent> createQueryResponse(QueryResponseContent value) {
        return new JAXBElement<QueryResponseContent>(_QueryResponse_QNAME, QueryResponseContent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ModifyResponseContent }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://maxgigapop.net/versans/nps/api/", name = "modifyResponse")
    public JAXBElement<ModifyResponseContent> createModifyResponse(ModifyResponseContent value) {
        return new JAXBElement<ModifyResponseContent>(_ModifyResponse_QNAME, ModifyResponseContent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetupContent }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://maxgigapop.net/versans/nps/api/", name = "setup")
    public JAXBElement<SetupContent> createSetup(SetupContent value) {
        return new JAXBElement<SetupContent>(_Setup_QNAME, SetupContent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetupResponseContent }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://maxgigapop.net/versans/nps/api/", name = "setupResponse")
    public JAXBElement<SetupResponseContent> createSetupResponse(SetupResponseContent value) {
        return new JAXBElement<SetupResponseContent>(_SetupResponse_QNAME, SetupResponseContent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryContent }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://maxgigapop.net/versans/nps/api/", name = "query")
    public JAXBElement<QueryContent> createQuery(QueryContent value) {
        return new JAXBElement<QueryContent>(_Query_QNAME, QueryContent.class, null, value);
    }

}
