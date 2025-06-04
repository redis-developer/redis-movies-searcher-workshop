document.addEventListener('DOMContentLoaded', () => {
    const searchBox = document.getElementById('search-box');
    const searchResults = document.querySelector('.search-results');
    const resultsTableBody = document.querySelector('#results-table tbody');

    // Initially hide the search-results div
    searchResults.style.display = 'none';

    // Function to perform the search
    const performSearch = async () => {
        const query = cleanQuery(searchBox.value.trim());
        const timeTakenDiv = document.getElementById('time-taken');

        if (query.length > 0) {
            try {
                // Start timing
                const startTime = performance.now();

                // Fetch data from the API
                const response = await fetch(`${searchAPI}?query=${encodeURIComponent(query)}`);
                if (!response.ok) {
                    throw new Error('Failed to fetch search results');
                }

                const data = await response.json();
                // End timing
                const endTime = performance.now();
                const duration = (endTime - startTime).toFixed(2);

                let formattedDuration;
                if (duration >= 60000) {
                    formattedDuration = `${(duration / 60000).toFixed(1)} m`;
                } else if (duration >= 1000) {
                    formattedDuration = `${(duration / 1000).toFixed(1)} s`;
                } else {
                    formattedDuration = `${duration} ms`;
                }

                // Display the time taken
                timeTakenDiv.textContent = `${data.resultType} → ${formattedDuration}`;

                // Clear previous results
                resultsTableBody.innerHTML = '';

                // Populate the table with new results
                if (data.matchedMovies && data.matchedMovies.length > 0) {
                    data.matchedMovies.forEach(result => {
                        const row = document.createElement('tr');
                        row.innerHTML = `
                        <td style="border: 1px solid #ddd; padding: 8px; text-align: center;">${result.year}</td>
                        <td style="border: 1px solid #ddd; padding: 8px;">${result.title}</td>
                        <td style="border: 1px solid #ddd; padding: 8px;">${result.plot}</td>
                        <td style="border: 1px solid #ddd; padding: 8px;">${result.actors}</td>
                        <td style="border: 1px solid #ddd; padding: 8px; text-align: center;">${result.rating}</td>
                    `;
                        resultsTableBody.appendChild(row);
                    });

                    // Show the search-results div if there are rows
                    searchResults.style.display = 'block';
                } else {
                    // Hide the search-results div if no results
                    searchResults.style.display = 'none';
                    timeTakenDiv.textContent = '';
                }
            } catch (error) {
                console.error('Error fetching search results:', error);
                alert('An error occurred while fetching search results. Please try again.');
                timeTakenDiv.textContent = '';
            }
        } else {
            // Hide the search-results div if the query is empty
            searchResults.style.display = 'none';
            timeTakenDiv.textContent = ''; // Clear time if query is empty
        }
    };

    // Debounce function to limit how often performSearch is called
    const debounce = (func, delay) => {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => func(...args), delay);
        };
    };

    // Trigger search on input event with debounce
    searchBox.addEventListener('input', debounce(performSearch, 250));
});

function cleanQuery(query) {
    return query.replace(/[^a-zA-Z0-9\s]/g, '').trim();
}
