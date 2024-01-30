(ns wkok.openai-clojure.azure-test
  (:require
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]
   [martian.core :as martian]
   [wkok.openai-clojure.azure :as azure]))

(deftest request-format
  (testing "request has the correct format"
    (let [spec-version (-> (azure/load-openai-spec) :info :version)
          model "text-davinci-003"
          params {:model model
                  :prompt "Say this is a test"
                  :max_tokens 7
                  :temperature 0}
          request (martian/request-for @azure/m :create-completion
                                       (azure/patch-params params))]

      (testing "api-version matches spec"
        (is (= spec-version
               (-> request :query-params :api-version))))

      (testing "contains api-key header"
        (is (contains? (:headers request) "Authorization")))

      (testing "deployment-id in body"
        (is (string/includes? (:body request) model)))

      (testing "params patched correctly"
        (is (= {:api-version spec-version
                :martian.core/body
                (assoc (dissoc params :model)
                       :deployment-id (:model params))}
               (azure/patch-params params)))))))

(deftest add-headers-init
  (let [add-headers-fn (-> azure/add-authentication-header :enter)]
    (testing "atoms get initialized correctly"

      (is (= {"Authorization" "Bearer my-secret-key"}
             (-> (add-headers-fn {:params {:wkok.openai-clojure.core/options {:api-key "my-secret-key"}}})
                 :request
                 :headers))))))

(deftest override-api-endpoint-test
  (let [override-api-endpoint-fn (-> azure/override-api-endpoint :enter)]
    (testing "api endpoint gets correctly overridden"

      (let [api-endpoint "https://myendpoint.openai.azure.com"
            path "/openai/some/chat/prompt"
            test-fn (fn [url]
                      (is (= (str api-endpoint path)
                             (-> (override-api-endpoint-fn {:request {:url url}
                                                            :params {:wkok.openai-clojure.core/options {:api-endpoint api-endpoint}}})
                                 :request
                                 :url))))]
        (test-fn "/openai/some/chat/prompt")
        (test-fn "https://www.some-other-endpoint.com/openai/some/chat/prompt")))
    (testing "api endpoint gets correctly not overridden"

      (let [api-endpoint "https://myendpoint.openai.azure.com"
            path "/some/chat/prompt"
            test-fn (fn [url]
                      (is (= (str api-endpoint path)
                             (-> (override-api-endpoint-fn {:request {:url url}
                                                            :params {:wkok.openai-clojure.core/options {:api-endpoint api-endpoint}}})
                                 :request
                                 :url))))]
        (test-fn "/some/chat/prompt")
        (test-fn "https://www.some-other-endpoint.com/some/chat/prompt")))))
