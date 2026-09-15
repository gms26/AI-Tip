import axiosInstance from './axios';

const getCoaching = async (currency) => {
    let url = '/api/tip-coach';
    if (currency) {
        url += `?currency=${currency}`;
    }
    const response = await axiosInstance.get(url);
    return response.data;
};

export default {
    getCoaching
};
