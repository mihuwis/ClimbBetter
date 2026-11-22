import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './router';
import { AuthProvider } from '../features/auth/context/AuthProvider';
export default function App() { return <BrowserRouter><AuthProvider><AppRoutes/></AuthProvider></BrowserRouter>; }
